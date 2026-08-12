package com.example.cache.stampede;

import com.example.cache.support.CacheLogStore;
import com.example.cache.support.HotDataService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 06. 缓存穿透 / 击穿 / 雪崩：三大经典问题的现场与应对。
 *
 * - 穿透：查一个「缓存和库里都没有」的 key，每次都打到 DB（恶意刷不存在 id）；
 * - 击穿：某个「热点 key」正好过期，瞬间大量请求同时打到 DB（本模块现场复现）；
 * - 雪崩：大量 key 同时过期（或缓存服务挂了），请求集体压到 DB。
 *
 * 应对（面试必背）：
 * - 穿透：空值也缓存（短 TTL）+ 参数校验 + 布隆过滤器；
 * - 击穿：单飞（single-flight，多线程只放一个去加载，其余等待）+ 逻辑过期 + 分布式锁；
 * - 雪崩：TTL 加随机抖动 + 多级缓存 + 限流熔断 + 缓存服务高可用。
 *
 * 本模块的 singleFlightGet 就是一个自实现单飞（Caffeine LoadingCache.get 底层也是这个思路）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StampedeService {

    private static final Object NULL_SENTINEL = new Object();

    private final Cache<String, Object> preheatCache;
    private final HotDataService hotDataService;
    private final CacheLogStore logStore;

    /** 单飞：进行中的加载 future，同一 key 只放一个加载任务 */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> inflight = new ConcurrentHashMap<>();

    private final AtomicLong singleFlightLoads = new AtomicLong();

    /**
     * 穿透演示：不存在的 key，缓存空值（短 TTL）能挡住大部分 DB 查询。
     */
    public Map<String, Object> nullDemo(int times) {
        int safe = Math.max(5, Math.min(times, 500));
        int absentId = 9999; // 库里不存在

        // 方案 A：不缓存 null —— 每次请求都查库
        Cache<String, Object> noNull = Caffeine.newBuilder().build();
        int dbLoadsNoNull = 0;
        for (int i = 0; i < safe; i++) {
            if (noNull.getIfPresent(HotDataService.KEY_PREFIX + absentId) == null) {
                hotDataService.loadUser(absentId); // 返回 null
                dbLoadsNoNull++;
            }
        }

        // 方案 B：空值也缓存（短 TTL）—— 只有第一次查库
        Cache<String, Object> withNull = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.SECONDS)
                .build();
        int dbLoadsWithNull = 0;
        for (int i = 0; i < safe; i++) {
            Object value = withNull.getIfPresent(HotDataService.KEY_PREFIX + absentId);
            if (value == null) {
                Object loaded = hotDataService.loadUser(absentId);
                withNull.put(HotDataService.KEY_PREFIX + absentId, loaded == null ? NULL_SENTINEL : loaded);
                dbLoadsWithNull++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("times", safe);
        result.put("absentId", absentId);
        result.put("dbLoadsWithoutNullCache", dbLoadsNoNull);
        result.put("dbLoadsWithNullCache", dbLoadsWithNull);
        result.put("tip", "不存在的 key 不缓存 null → " + safe + " 次请求全打到 DB（穿透）；"
                + "空值缓存 + 短 TTL → 只查 1 次库，其余命中。注意空值缓存 TTL 要比正常数据短。");

        logStore.add("stampede", "null-demo", HotDataService.KEY_PREFIX + absentId, null, "空值缓存防穿透");
        return result;
    }

    /**
     * 击穿现场：热点 key 过期瞬间，N 个线程没有保护地同时打到 DB。
     */
    public Map<String, Object> stampedeDemo(int threads) {
        int n = Math.max(2, Math.min(threads, 100));
        // 模拟热点 key 刚过期：把 user:1 从缓存清掉
        preheatCache.invalidate(HotDataService.KEY_PREFIX + 1);
        int before = hotDataService.stampedeLoadCount();

        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                try {
                    ready.await();
                    hotDataService.loadUserForStampede(1); // 无保护：每个线程都查库
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        ready.countDown();
        await(done);
        int dbLoads = hotDataService.stampedeLoadCount() - before;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("threads", n);
        result.put("hotKey", HotDataService.KEY_PREFIX + 1);
        result.put("dbLoads", dbLoads);
        result.put("tip", "热点 key 过期瞬间，N 个并发请求全部 miss、全部打到 DB（" + dbLoads + " 次）——这就是「击穿」。"
                + "数据库抗不住就雪崩了。看下一个场景：单飞怎么救。");

        logStore.add("stampede", "stampede-demo", HotDataService.KEY_PREFIX + 1, null, "击穿现场 " + n + " 线程");
        return result;
    }

    /**
     * 单飞：N 个线程并发请求同一个 key，只放一个去加载，其余等待结果。
     */
    public Map<String, Object> singleflight(int threads) {
        int n = Math.max(2, Math.min(threads, 100));
        singleFlightLoads.set(0);
        preheatCache.invalidate(HotDataService.KEY_PREFIX + 1);

        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                try {
                    ready.await();
                    singleFlightGet(HotDataService.KEY_PREFIX + 1); // 有保护：合并并发
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        ready.countDown();
        await(done);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("threads", n);
        result.put("hotKey", HotDataService.KEY_PREFIX + 1);
        result.put("actualLoads", singleFlightLoads.get());
        result.put("tip", "单飞（single-flight）：同一 key 的并发请求被合并成 1 次加载，其余线程等待同一个结果。"
                + "「实际只查库 1 次」——这就是击穿的标准解法，Caffeine LoadingCache.get 内部也是这么做的。");

        logStore.add("stampede", "singleflight", HotDataService.KEY_PREFIX + 1, null, "单飞合并 " + n + " 并发");
        return result;
    }

    /**
     * 单飞实现（手写标准姿势）：
     * - inflight.get(key)：无锁读，拿到占位 future 直接 join（并发线程共享同一个结果）；
     * - putIfAbsent：只有第一个线程能放进自己的 future，其余拿到别人的直接复用；
     * - 加载者完成后用 <b>remove(key, future)</b>（带值校验）清理占位——
     *   只删自己这轮的 future，不会误删下一轮；等待者早已持有 future 引用，删不删都不影响它们 join。
     */
    private Object singleFlightGet(String key) {
        CompletableFuture<Object> future = inflight.get(key);
        if (future == null) {
            CompletableFuture<Object> created = new CompletableFuture<>();
            CompletableFuture<Object> existing = inflight.putIfAbsent(key, created);
            if (existing != null) {
                future = existing; // 别人抢先占位了，等他的结果
            } else {
                // 我是加载者
                singleFlightLoads.incrementAndGet();
                try {
                    Object value = hotDataService.loadUser(key);
                    created.complete(value);
                } catch (Exception e) {
                    created.completeExceptionally(e);
                } finally {
                    // 带值校验删除：只清自己这轮；下一轮请求会开启新的加载
                    inflight.remove(key, created);
                }
                return created.join();
            }
        }
        return future.join();
    }

    /**
     * 三大问题速记（八股）。
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("problems", new LinkedHashMap<String, Object>() {{
            put("穿透", "查「缓存和库都没有」的 key，每次都打 DB；恶意刷不存在 id 能把 DB 打挂");
            put("击穿", "热点 key 正好过期，瞬间大量请求同时 miss 打 DB（本模块现场复现）");
            put("雪崩", "大量 key 同时过期 / 缓存服务宕机，请求集体压向 DB");
        }});
        result.put("solutions", new LinkedHashMap<String, Object>() {{
            put("穿透", "空值缓存(短TTL) + 入参校验 + 布隆过滤器挡不存在 id");
            put("击穿", "单飞/互斥（多线程合并成一次加载）+ 逻辑过期 + 分布式锁");
            put("雪崩", "TTL 加随机抖动 + 多级缓存(本地+Redis) + 限流熔断 + 缓存高可用(哨兵/集群)");
        }});
        result.put("tip", "三个问题解决思路各不相同：穿透是「挡」，击穿是「合并」，雪崩是「错峰+兜底」。");
        return result;
    }

    /**
     * 单飞细节速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("singleflight", new String[]{
                "思想：同一 key 的并发加载只放一个去执行，其余线程等待（合并请求）",
                "实现：ConcurrentHashMap<String, CompletableFuture> + computeIfAbsent（本模块 singleFlightGet）",
                "替代：Caffeine LoadingCache.get 自带单飞；跨进程场景用分布式锁 + 双检"
        });
        result.put("logicExpire", "逻辑过期：缓存里不放真实 TTL，存「过期时间戳」字段，读到时发现过期 → 只让一个线程重建，其余先返回旧值。"
                + "比物理过期更平滑，适合秒杀热点。");
        result.put("tip", "单飞要小心死锁：加载过程里别再回调同一个 key 的加载；加载失败要能重试，别把 future 永久留在 map 里。");
        return result;
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

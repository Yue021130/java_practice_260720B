package com.example.cache.refresh;

import com.example.cache.config.CachePracticeProperties;
import com.example.cache.support.CacheLogStore;
import com.example.cache.support.HotDataService;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 03. 刷新与异步：refreshAfterWrite 定时刷新 / AsyncCache 异步加载。
 *
 * 关键区别（面试必问）：
 * - expireAfterWrite：过期后下次读必 miss，要重新加载，慢；
 * - refreshAfterWrite：过期后「旧值仍可用」，后台异步刷新新值，读不阻塞。
 *   所以生产上常用 refreshAfterWrite + 较长的 expireAfterWrite 组合，
 *   既保证数据不太旧，又避免「过期瞬间的缓存击穿」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshService {

    private final LoadingCache<String, Object> loadingCache;
    private final HotDataService hotDataService;
    private final CachePracticeProperties props;
    private final CacheLogStore logStore;

    /**
     * 刷新演示：refreshAfterWrite 到期后读取不阻塞、后台异步刷新。
     */
    public Map<String, Object> refreshDemo(long waitMs) {
        String key = HotDataService.KEY_PREFIX + 1;
        long refreshInterval = props.getRefreshAfterWriteMs();
        // 等的时间要盖过刷新间隔，才看得到「刷新发生但读不阻塞」
        long wait = Math.max(refreshInterval + 100, Math.min(waitMs, 10_000));

        int beforeLoads = hotDataService.userLoadCount();

        loadingCache.get(key);                         // 第一次：miss → 加载
        long secondMs = cost(() -> loadingCache.get(key)); // 第二次：hit

        sleep(wait);                                   // 超过刷新间隔

        long refreshMs = cost(() -> loadingCache.get(key)); // 第三次：触发异步刷新，读仍秒回
        sleep(200);                                    // 等后台刷新落库

        int loads = hotDataService.userLoadCount() - beforeLoads;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("refreshAfterWriteMs", refreshInterval);
        result.put("waitedMs", wait);
        result.put("secondReadMs", secondMs);
        result.put("refreshedReadMs", refreshMs);
        result.put("dbLoads", loads);
        result.put("tip", "第三次读时旧值已过期，但 refreshAfterWrite 让它「先用旧值秒回、后台异步刷新」"
                + "（读耗时 " + refreshMs + "ms，DB 被多查 1 次）。这就是「读不阻塞 + 数据不过期」的双赢。");

        logStore.add("refresh", "refresh-demo", key, null, "refreshAfterWrite 异步刷新");
        return result;
    }

    /**
     * 异步缓存：AsyncCache 返回 CompletableFuture，加载在别的线程完成。
     */
    public Map<String, Object> asyncDemo(int id) {
        String key = HotDataService.KEY_PREFIX + id;
        AsyncCache<String, Object> asyncCache = Caffeine.newBuilder()
                .maximumSize(props.getMaxSize())
                .buildAsync();

        long t1 = System.currentTimeMillis();
        // 提交异步加载：立即返回 future，不阻塞当前线程
        CompletableFuture<Object> future = asyncCache.get(key, k -> hotDataService.loadUser(k));
        long submitMs = System.currentTimeMillis() - t1;

        Object value;
        try {
            value = future.get();       // 阻塞等结果（真实业务这里可以并发做别的事）
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("异步加载失败：" + e.getMessage(), e);
        }

        Object hit = asyncCache.synchronous().getIfPresent(key);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("submitCostMs", submitMs);
        result.put("value", value);
        result.put("asyncHitOnSecondRead", hit != null);
        result.put("tip", "asyncCache.get() 立即返回 CompletableFuture（提交 " + submitMs + "ms），"
                + "加载在线程池完成；synchronous() 拿到同步视图，第二次读直接命中。");

        logStore.add("refresh", "async-demo", key, hit != null, "AsyncCache 异步加载");
        return result;
    }

    /**
     * 刷新与异步速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("refreshVsExpire", new LinkedHashMap<String, Object>() {{
            put("expireAfterWrite", "过期即失效，下次读 miss 重新加载：适合变化慢、可接受偶发慢读");
            put("refreshAfterWrite", "过期后旧值仍可用，后台异步刷新：读永不 miss、数据不过期");
            put("组合", "refreshAfterWrite(短) + expireAfterWrite(长)：常用黄金组合，防击穿又保新鲜");
        }});
        result.put("async", new String[]{
                "AsyncCache / AsyncLoadingCache：get 返回 CompletableFuture，加载在线程池执行",
                "适合：加载很慢（跨 RPC/复杂聚合）且不想阻塞请求线程的场景",
                "注意：异步加载失败要兜底（future.completeExceptionally 后调用方感知），别让异常静默丢"
        });
        result.put("tip", "refreshAfterWrite 只在「读已存在的 key」时触发刷新，miss 时仍走正常加载。");
        return result;
    }

    private long cost(Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        return System.currentTimeMillis() - start;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(Math.max(1, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

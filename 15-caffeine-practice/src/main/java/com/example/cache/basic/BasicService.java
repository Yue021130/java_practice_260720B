package com.example.cache.basic;

import com.example.cache.support.CacheLogStore;
import com.example.cache.support.HotDataService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 01. 快速开始：Caffeine 是什么 / 手动 Cache / LoadingCache 自动加载。
 *
 * 两种用法的区别（面试必问）：
 * - Cache：只管存储，读 miss 后由你决定「要不要查库、放不放缓存」；
 * - LoadingCache：绑定 CacheLoader，get(key) 未命中时自动调用 loader 加载并回填。
 *   LoadingCache 在并发下同一 key 只加载一次（自带「单飞」），天然防缓存击穿。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BasicService {

    private final Cache<String, Object> demoCache;
    private final LoadingCache<String, Object> loadingCache;
    private final HotDataService hotDataService;
    private final CacheLogStore logStore;

    /**
     * 手动 Cache 全流程：miss → 查库 → put → hit → invalidate。
     */
    public Map<String, Object> cacheDemo(int id) {
        String key = HotDataService.KEY_PREFIX + id;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);

        // 1. 第一次读：miss
        long t1 = System.nanoTime();
        Object miss = demoCache.getIfPresent(key);
        long missCost = (System.nanoTime() - t1) / 1_000_000;

        // 2. 没命中 → 查库 → 放进缓存
        Map<String, Object> user = hotDataService.loadUser(id);
        demoCache.put(key, user);

        // 3. 第二次读：hit（不查库，几乎 0 耗时）
        long t2 = System.nanoTime();
        Object hit = demoCache.getIfPresent(key);
        long hitCost = (System.nanoTime() - t2) / 1_000_000;

        // 4. 失效
        demoCache.invalidate(key);
        Object afterInvalidate = demoCache.getIfPresent(key);

        result.put("firstReadMiss", miss == null);
        result.put("dbLoadMs", missCost);
        result.put("secondReadHit", hit != null);
        result.put("cacheHitMs", hitCost);
        result.put("afterInvalidate", afterInvalidate);
        result.put("tip", "miss 要查库（" + missCost + "ms+），hit 只走内存（" + hitCost + "ms）；"
                + "invalidate 后再次读又会 miss。");

        logStore.add("basic", "cache-demo", key, hit != null, "手动 Cache 全流程");
        return result;
    }

    /**
     * LoadingCache：get(key) 未命中自动走 CacheLoader 加载并回填。
     */
    public Map<String, Object> loading(int id) {
        String key = HotDataService.KEY_PREFIX + id;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);

        int beforeLoads = hotDataService.userLoadCount();
        long t1 = System.currentTimeMillis();
        Object first = loadingCache.get(key);      // 第一次：miss → loader 自动加载
        long firstMs = System.currentTimeMillis() - t1;

        long t2 = System.currentTimeMillis();
        Object second = loadingCache.get(key);     // 第二次：hit
        long secondMs = System.currentTimeMillis() - t2;

        result.put("firstValue", first);
        result.put("firstCostMs", firstMs);
        result.put("secondValue", second);
        result.put("secondCostMs", secondMs);
        result.put("dbLoadsDelta", hotDataService.userLoadCount() - beforeLoads);
        result.put("tip", "LoadingCache.get() 未命中自动加载并回填：第一次查库(" + firstMs + "ms)，"
                + "第二次纯内存(" + secondMs + "ms)，DB 只被查了 1 次。");

        logStore.add("basic", "loading", key, true, "LoadingCache 自动加载");
        return result;
    }

    /**
     * 核心概念速记（八股）。
     */
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("what", "Caffeine 是一个高性能 Java 本地缓存库（Guava Cache 的继任者），"
                + "基于 Window-TinyLFU 淘汰算法，号称「并发吞吐最高的进程内缓存」");
        result.put("why", new String[]{
                "性能：Window-TinyLFU 兼顾命中率与并发度，吞吐是 Guava Cache 的几倍",
                "功能：容量/时间/引用淘汰、自动刷新、异步加载、统计、监听器齐全",
                "生态：Spring Boot 原生集成（spring-boot-starter-cache + CaffeineCacheManager）"
        });
        result.put("vs", new LinkedHashMap<String, Object>() {{
            put("vs Guava Cache", "Caffeine 是 Guava Cache 作者的续作，算法更新、更快，新项目直接选 Caffeine");
            put("vs Redis", "Caffeine 是 JVM 进程内缓存（多实例各自一份、快但各机不一致）；Redis 是分布式缓存（跨实例共享、要网络开销）");
        }});
        result.put("api", new LinkedHashMap<String, Object>() {{
            put("Cache", "Cache.getIfPresent / put / invalidate，miss 后自行决定加载");
            put("LoadingCache", "绑定 CacheLoader，get 未命中自动加载并回填，并发只加载一次");
            put("AsyncCache / AsyncLoadingCache", "异步版本，返回 CompletableFuture（见 03 章）");
        }});
        result.put("tip", "本地缓存适合：单机热点、读多写少、可接受短暂不一致的数据（字典、配置、热门数据）。");
        return result;
    }
}

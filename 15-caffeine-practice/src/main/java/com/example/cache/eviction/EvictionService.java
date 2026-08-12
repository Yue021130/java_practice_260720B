package com.example.cache.eviction;

import com.example.cache.support.CacheLogStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 02. 淘汰策略：容量淘汰（maximumSize）/ 时间淘汰（expireAfterWrite / expireAfterAccess）。
 *
 * Caffeine 三类淘汰策略（面试必背）：
 * 1. 容量：maximumSize / maximumWeight —— 满了按 Window-TinyLFU 淘汰低频的；
 * 2. 时间：expireAfterWrite（写后过期）/ expireAfterAccess（访问后过期）/ expireAfter（自定义）；
 * 3. 引用：weakKeys / weakValues / softValues —— 交给 GC。
 *
 * 注意：Caffeine 的容量淘汰不是严格 LRU（是 TinyLFU 近似 LRU），
 * 所以「最后放进去的 5 个一定存活」这个直觉并不严格成立，只保证高频的活下来。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvictionService {

    private final Cache<String, String> evictSizeCache;
    private final CacheLogStore logStore;

    /**
     * 容量淘汰：maximumSize=5，放进 count 个，看还剩多少、淘汰了多少。
     */
    public Map<String, Object> sizeDemo(int count) {
        int safeCount = Math.max(1, Math.min(count, 100));
        evictSizeCache.invalidateAll();
        for (int i = 1; i <= safeCount; i++) {
            evictSizeCache.put("key:" + i, "value:" + i);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maximumSize", 5);
        result.put("putCount", safeCount);
        result.put("estimatedSize", evictSizeCache.estimatedSize());
        result.put("evictionCount", evictSizeCache.stats().evictionCount());
        evictSizeCache.policy().eviction().ifPresent(e -> {
            result.put("policyMaximum", e.getMaximum());
            result.put("weightedSize", e.weightedSize());
        });
        result.put("survivedKeys", evictSizeCache.asMap().keySet());
        result.put("tip", "放进 " + safeCount + " 个只留 ~5 个，被淘汰的是 TinyLFU 认为最不常用的；"
                + "注意不是严格 LRU，别指望「最后放的必然存活」。");

        logStore.add("eviction", "size", "key:*", null, "容量淘汰 maximumSize=5");
        return result;
    }

    /**
     * 时间淘汰：expireAfterWrite vs expireAfterAccess 的关键差异演示。
     *
     * - write：写入后 duration 毫秒必过期，期间读不续命；
     * - access：每次读取都会「续命」，只要一直有人读就不过期。
     */
    public Map<String, Object> expireDemo(String type, long durationMs) {
        long dur = Math.max(50, Math.min(durationMs, 5000));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("durationMs", dur);

        if ("access".equalsIgnoreCase(type)) {
            // expireAfterAccess：持续访问 → 一直续命，总时长超过 duration 仍存活
            Cache<String, String> cache = Caffeine.newBuilder()
                    .expireAfterAccess(dur, TimeUnit.MILLISECONDS)
                    .build();
            cache.put("k", "v");
            int accesses = 0;
            long deadline = System.currentTimeMillis() + dur * 2;
            while (System.currentTimeMillis() < deadline) {
                cache.getIfPresent("k");
                accesses++;
                sleep(dur / 3);
            }
            result.put("accessedTimes", accesses);
            result.put("finalValueAfter_2x_duration", cache.getIfPresent("k"));
            result.put("tip", "expireAfterAccess：每次读取都续命，即使总时长已经超过 duration 依然存活（" + accesses + " 次访问续命）。");
        } else {
            // expireAfterWrite：写入后固定过期，读不续命
            Cache<String, String> cache = Caffeine.newBuilder()
                    .expireAfterWrite(dur, TimeUnit.MILLISECONDS)
                    .build();
            cache.put("k", "v");
            cache.getIfPresent("k"); // 读一次，但 write 策略不续命
            sleep(dur + 100);
            result.put("finalValueAfter_duration", cache.getIfPresent("k"));
            result.put("tip", "expireAfterWrite：写入后 " + dur + "ms 必过期，期间读多少次都不续命——适合「固定周期失效」的数据。");
        }

        logStore.add("eviction", "expire", "k", null, "时间淘汰 expireAfter" + type);
        return result;
    }

    /**
     * 淘汰策略速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategies", new LinkedHashMap<String, Object>() {{
            put("容量 maximumSize", "满了按 Window-TinyLFU 淘汰低频 key，O(1) 维护、命中率优于 LRU");
            put("时间 expireAfterWrite", "写后固定时长过期，期间读不续命：适合定时刷新的配置/字典");
            put("时间 expireAfterAccess", "访问后固定时长过期，读会续命：适合「不活跃就淘汰」的会话类数据");
            put("时间 expireAfter", "自定义过期策略（Expiry 接口），可按 key 动态给不同 TTL");
            put("引用 weakKeys/weakValues/softValues", "交给 GC 回收：weak 适合与外部引用共享生命周期，soft 适合大对象兜底（不推荐常用）");
        }});
        result.put("note", "真实业务 TTL 几乎都是 expireAfterWrite + 适当 refreshAfterWrite，因为「访问续命」容易让冷门数据赖着不走。");
        result.put("tip", "淘汰是异步/惰性发生的：estimatedSize() 可能短暂大于 maximumSize，这是正常现象（Caffeine 2.x 设计）。");
        return result;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(Math.max(1, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

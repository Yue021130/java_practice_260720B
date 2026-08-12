package com.example.cache.support;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 「Redis」模拟器：内存 Map + TTL，用于两级缓存（07 章）演示。
 *
 * 真实工程这里是 Redis（String/Hash 结构 + expire）；
 * 本专题为保持开箱即用，用 ConcurrentHashMap 模拟，读写语义一致：
 * - get：命中且未过期返回，否则视为 miss
 * - put(key, value, ttlMs)：ttlMs <= 0 表示永不过期
 * - delete / clear
 *
 * 同样维护 hit/miss 计数，方便对比 L1 Caffeine 与 L2 Redis 的命中分布。
 */
@Slf4j
@Component
public class SimpleRedisCache {

    @Data
    private static class Entry {
        private final Object value;
        private final long expireAt; // -1 表示永不过期
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();

    public Object get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            missCount.incrementAndGet();
            return null;
        }
        if (entry.getExpireAt() != -1 && System.currentTimeMillis() > entry.getExpireAt()) {
            store.remove(key);
            missCount.incrementAndGet();
            return null;
        }
        hitCount.incrementAndGet();
        return entry.getValue();
    }

    public void put(String key, Object value, long ttlMs) {
        long expireAt = ttlMs <= 0 ? -1 : System.currentTimeMillis() + ttlMs;
        store.put(key, new Entry(value, expireAt));
    }

    public void delete(String key) {
        store.remove(key);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long hits = hitCount.get();
        long misses = missCount.get();
        result.put("hits", hits);
        result.put("misses", misses);
        result.put("size", store.size());
        result.put("hitRate", hits + misses == 0 ? 0.0 : (double) hits / (hits + misses));
        return result;
    }
}

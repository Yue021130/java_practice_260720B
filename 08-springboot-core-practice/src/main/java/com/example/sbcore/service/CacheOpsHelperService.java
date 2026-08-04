package com.example.sbcore.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CacheOpsHelperService {

    private final ConcurrentHashMap<String, String> db = new ConcurrentHashMap<>();
    private final AtomicInteger callCount = new AtomicInteger(0);

    public CacheOpsHelperService() {
        db.put("key1", "初始值");
    }

    @Cacheable(value = "ops", key = "#key")
    public String cacheable(String key) {
        callCount.incrementAndGet();
        return db.get(key);
    }

    @CachePut(value = "ops", key = "#key")
    public String cachePut(String key, String value) {
        callCount.incrementAndGet();
        db.put(key, value);
        return value;
    }

    @CacheEvict(value = "ops", key = "#key")
    public void cacheEvict(String key) {
        db.remove(key);
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void resetCallCount() {
        callCount.set(0);
    }
}

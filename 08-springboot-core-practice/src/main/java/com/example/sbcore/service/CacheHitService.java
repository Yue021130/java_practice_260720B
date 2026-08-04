package com.example.sbcore.service;

import com.example.sbcore.cache.BookRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class CacheHitService {

    @Autowired
    private CacheHitHelperService helper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CacheManager cacheManager;

    public String getWithoutCache(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    public Map<String, Object> run(int totalRequests) {
        Map<String, Object> data = new LinkedHashMap<>();

        List<String> isbns = Arrays.asList("ISBN-001", "ISBN-002", "ISBN-003", "ISBN-004", "ISBN-005");
        int requests = Math.max(10, Math.min(totalRequests, 1000));

        // with cache
        bookRepository.resetCallCount();
        long withCacheStart = System.currentTimeMillis();
        int hits = 0;
        for (int i = 0; i < requests; i++) {
            String isbn = isbns.get(ThreadLocalRandom.current().nextInt(isbns.size()));
            String before = peekCache(isbn);
            helper.getWithCache(isbn);
            if (before != null) hits++;
        }
        long withCacheCost = System.currentTimeMillis() - withCacheStart;
        int withCacheDbCalls = bookRepository.getCallCount();

        // clear cache and test without cache
        org.springframework.cache.Cache hitCache = cacheManager.getCache("hit");
        if (hitCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) hitCache.getNativeCache();
            nativeCache.invalidateAll();
        }

        bookRepository.resetCallCount();
        long withoutCacheStart = System.currentTimeMillis();
        for (int i = 0; i < requests; i++) {
            String isbn = isbns.get(ThreadLocalRandom.current().nextInt(isbns.size()));
            getWithoutCache(isbn);
        }
        long withoutCacheCost = System.currentTimeMillis() - withoutCacheStart;
        int withoutCacheDbCalls = bookRepository.getCallCount();

        double hitRate = requests == 0 ? 0 : (double) hits / requests;

        data.put("totalRequests", requests);
        data.put("hitCount", hits);
        data.put("hitRate", String.format("%.2f%%", hitRate * 100));
        data.put("dbCallsWithCache", withCacheDbCalls);
        data.put("dbCallsWithoutCache", withoutCacheDbCalls);
        data.put("avgWithCacheMs", String.format("%.2f", (double) withCacheCost / requests));
        data.put("avgWithoutCacheMs", String.format("%.2f", (double) withoutCacheCost / requests));

        data.put("interviewNote",
                "缓存命中率 = 命中次数 / 总请求。命中率越高，DB 压力越小、响应越快。" +
                "缓存穿透指缓存与 DB 都不存在的 key 被高频请求，可通过缓存空值、布隆过滤器、接口校验缓解。");

        return data;
    }

    private String peekCache(String isbn) {
        org.springframework.cache.Cache cache = cacheManager.getCache("hit");
        if (cache == null) return null;
        org.springframework.cache.Cache.ValueWrapper wrapper = cache.get(isbn);
        return wrapper == null ? null : (String) wrapper.get();
    }
}

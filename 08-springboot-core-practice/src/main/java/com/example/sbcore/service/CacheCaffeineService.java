package com.example.sbcore.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class CacheCaffeineService {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private BookService bookService;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        bookService.resetInvocationCount();

        String first = bookService.getBookTitle("ISBN-001");
        String second = bookService.getBookTitle("ISBN-001");
        String third = bookService.getBookTitle("ISBN-NOT-EXIST");
        String fourth = bookService.getBookTitle("ISBN-002");

        Cache<Object, Object> nativeCache = (Cache<Object, Object>) cacheManager.getCache("books").getNativeCache();

        data.put("firstCall", first);
        data.put("secondCall", second);
        data.put("missedCall", third);
        data.put("thirdCall", fourth);
        data.put("actualMethodInvocations", bookService.getInvocationCount());
        data.put("estimatedSize", nativeCache.estimatedSize());

        data.put("interviewNote",
                "@Cacheable：先查缓存，命中直接返回；未命中执行方法并放入缓存。" +
                "condition 决定哪些参数进入缓存逻辑，unless 可过滤空结果。" +
                "注意：同类内部自调用会绕过 Spring 代理，导致缓存注解失效；应通过注入的 Bean 调用。" +
                "Caffeine 是进程内本地缓存，低延迟、容量与过期策略丰富，适合单机读多写少。");

        return data;
    }
}

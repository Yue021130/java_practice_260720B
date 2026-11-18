package com.example.scp.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 二级缓存管理器：实现 Spring Cache 的 {@link CacheManager} 接口，
 * 让 {@code @Cacheable(value="product")} 里的 value 落到本类的二级缓存上。
 *
 * <p>每个 cacheName 惰性创建一个 {@link TwoLevelCache} 实例并缓存
 * （ConcurrentHashMap），相同 name 返回同一实例——这是 Spring Cache
 * 对 CacheManager 的约定。</p>
 *
 * <p>为什么没有直接用 RedisCacheManager / CaffeineCacheManager：
 * 生产常用「本地 + 远程」二级结构兼顾速度与容量，Spring Boot 开箱
 * 没有提供双级实现，需要自研——这正是本章的核心练习。</p>
 */
@ConfigurationProperties(prefix = "spring.cache.two-level")
public class TwoLevelCacheManager implements CacheManager {

    private final Map<String, TwoLevelCache> caches = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final TwoLevelCacheProperties properties;

    public TwoLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                TwoLevelCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name,
                n -> new TwoLevelCache(n, redisTemplate, properties));
    }

    /** 监控/管理用：按名字取二级缓存具体实现 */
    public TwoLevelCache getTwoLevelCache(String name) {
        return (TwoLevelCache) getCache(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }
}

package com.example.cache.config;

import com.example.cache.support.HotDataService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 缓存基础设施配置。
 *
 * 两条线并存，各司其职：
 * 1. <b>Spring Cache 抽象</b>（CacheManager）：给 @Cacheable / @CacheEvict / @CachePut 用，
 *    08 章与 09 章走这里；
 * 2. <b>原生 Caffeine API</b>（Cache / LoadingCache 直连）：其余各章直接操作 Caffeine，
 *    方便把「命中 / 淘汰 / 刷新 / 统计」讲透。
 *
 * 各模块用不同的构造参数（容量 / 过期 / 刷新 / 统计），
 * 体现 Caffeine 按需配置的灵活性。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CaffeineConfig {

    private final CachePracticeProperties props;

    /**
     * Spring Cache 抽象：命名缓存 users，供 @Cacheable 注解使用。
     *
     * 注意：CaffeineCacheManager 的 recordStats 对注解缓存生效，
     * 这样 08 章也能拿到命中率。
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(props.getMaxSize())
                .expireAfterWrite(props.getExpireAfterWriteMs(), TimeUnit.MILLISECONDS)
                .recordStats());
        manager.setCacheNames(Arrays.asList("users", "userCache"));
        log.info("Spring CacheManager 就绪：缓存名 = users / userCache");
        return manager;
    }

    /**
     * 01 快速开始 / 04 统计用：普通 Cache（getIfPresent / put / invalidate）。
     */
    @Bean("demoCache")
    public Cache<String, Object> demoCache() {
        return Caffeine.newBuilder()
                .maximumSize(props.getMaxSize())
                .expireAfterWrite(props.getExpireAfterWriteMs(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build();
    }

    /**
     * 01 加载型 / 03 刷新用：LoadingCache + CacheLoader。
     *
     * - refreshAfterWrite(2s)：读已存在的 key 超过 2 秒会异步刷新，
     *   刷新期间旧值立即可用（不阻塞），是「缓存击穿」的兜底手段之一；
     * - 刷新失败会保留旧值，不会抛异常把请求打挂。
     */
    @Bean("loadingCache")
    public LoadingCache<String, Object> loadingCache(HotDataService hotDataService) {
        return Caffeine.newBuilder()
                .maximumSize(props.getMaxSize())
                .expireAfterWrite(30_000, TimeUnit.MILLISECONDS)
                .refreshAfterWrite(props.getRefreshAfterWriteMs(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build(hotDataService::loadUser);
    }

    /**
     * 02 容量淘汰用：很小的 maximumSize(5)，一放就淘汰。
     */
    @Bean("evictSizeCache")
    public Cache<String, String> evictSizeCache() {
        return Caffeine.newBuilder()
                .maximumSize(5)
                .recordStats()
                .build();
    }

    /**
     * 05 缓存预热 / 06 穿透击穿用：带统计的普通 Cache。
     */
    @Bean("preheatCache")
    public Cache<String, Object> preheatCache() {
        return Caffeine.newBuilder()
                .maximumSize(props.getMaxSize())
                .expireAfterWrite(props.getExpireAfterWriteMs(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build();
    }

    /**
     * 07 两级缓存用：本地一级缓存（L1），容量小、过期短。
     */
    @Bean("l1Cache")
    public Cache<String, Object> l1Cache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(10_000, TimeUnit.MILLISECONDS)
                .recordStats()
                .build();
    }
}

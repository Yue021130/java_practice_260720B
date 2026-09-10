package com.example.ccp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Caffeine 缓存配置（本章主角）。
 *
 * <p>对应文档《Caffeine 核心配置全解》的核心结论：</p>
 * <ul>
 *   <li>使用 {@link CaffeineCacheManager} 接入 Spring Cache 抽象，业务代码用
 *       {@code @Cacheable / @CacheEvict} 注解即可，无需手动 new Caffeine 对象；</li>
 *   <li>容量（maximumSize）、过期策略（expireAfterWrite）、统计（recordStats）等核心参数
 *       集中在 application.yml 的 spring.cache.caffeine.spec 一行配置，本类负责解析并应用；</li>
 *   <li>Spring Cache 默认按方法参数生成 key、返回值整体作为 value，适合「读多写少」的用户详情类场景。</li>
 * </ul>
 */
@EnableCaching
@Configuration
public class CacheConfig {

    /** 与 application.yml 的 spring.cache.cache-names 保持一致 */
    private static final List<String> CACHE_NAMES = Arrays.asList("user", "userPage");

    /**
     * 从 yml 读取 spec 并解析，保证「改配置即生效」；
     * 若不显式构建此 Bean，也可删掉本类只用 @EnableCaching，交给 Spring Boot 自动装配。
     */
    @Bean
    public CacheManager cacheManager(@Value("${spring.cache.caffeine.spec}") String spec) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(CACHE_NAMES);
        manager.setCaffeine(Caffeine.from(CaffeineSpec.parse(spec)));
        // 允许缓存 null，避免缓存击穿时同一 key 的重复加载压垮数据库
        manager.setAllowNullValues(true);
        return manager;
    }
}

package com.example.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import com.example.cache.config.CachePracticeProperties;

/**
 * Spring Boot + Caffeine 缓存实践启动类。
 *
 * - @EnableCaching：开启 Spring Cache 抽象（@Cacheable / @CacheEvict / @CachePut）
 * - Caffeine 2.9.3：本地高性能缓存，由 Spring Boot 依赖管理锁定版本
 * - 全部场景开箱即用：不依赖任何外部服务，两级缓存里的 Redis 用内存 Map 模拟
 */
@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(CachePracticeProperties.class)
public class CaffeinePracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaffeinePracticeApplication.class, args);
    }
}

package com.example.scp.config;

import com.example.scp.cache.TwoLevelCacheManager;
import com.example.scp.cache.TwoLevelCacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Spring Cache 装配：把自定义二级缓存管理器挂进 Spring Cache 抽象。
 *
 * <p>关键点：声明一个 CacheManager 类型的 Bean 后，
 * CacheAutoConfiguration 的 @ConditionalOnMissingBean(CacheManager.class)
 * 会让 Boot 的自动选型（ConcurrentMap/Redis/Caffeine...）整体让位，
 * application.yml 中 spring.cache.type=none 只是双保险。</p>
 *
 * <p>同时注册自定义 KeyGenerator「productPageKeyGenerator」：默认
 * SimpleKeyGenerator 对多参数生成 SimpleKey 对象可读性差，分页查询这类
 * 复合参数场景自定义生成 "page:1:10:手机:-" 这样的可读 key，
 * 在 @Cacheable(keyGenerator="productPageKeyGenerator") 中按名引用。</p>
 */
@Configuration
@EnableConfigurationProperties(TwoLevelCacheProperties.class)
public class CacheConfig {

    @Bean
    public TwoLevelCacheManager cacheManager(RedisTemplate<String, Object> redisTemplate,
                                             TwoLevelCacheProperties properties) {
        return new TwoLevelCacheManager(redisTemplate, properties);
    }

    /**
     * 分页查询缓存 key 生成器：按参数顺序拼接，null 显示为 "-"。
     */
    @Bean("productPageKeyGenerator")
    public KeyGenerator productPageKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder("page");
            for (Object p : params) {
                sb.append(':').append(p == null ? "-" : p.toString());
            }
            return sb.toString();
        };
    }
}

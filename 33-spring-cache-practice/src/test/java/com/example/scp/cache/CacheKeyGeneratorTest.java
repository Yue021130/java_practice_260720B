package com.example.scp.cache;

import com.example.scp.config.CacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.KeyGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 自定义 KeyGenerator 测试：分页缓存 key 的可读性规则。
 *
 * <p>默认 SimpleKeyGenerator 生成 SimpleKey 对象 toString 是
 * "com.example...SimpleKey [1, 10, 手机, null]"，又长又难读；
 * 自定义生成器要求 "page:1:10:手机:-" 这样短小可读的 key，
 * 方便在 Redis 客户端和「缓存监控」页排查。</p>
 */
class CacheKeyGeneratorTest {

    private final KeyGenerator generator = new CacheConfig().productPageKeyGenerator();

    @Test
    @DisplayName("多参数按序拼接，null 显示为 -")
    void joinParams() {
        Object key = generator.generate(null, null, new Object[]{1L, 10L, "手机", null});
        assertEquals("page:1:10:手机:-", key.toString());
    }

    @Test
    @DisplayName("空查询条件的分页 key 也稳定可读")
    void emptyParams() {
        Object key = generator.generate(null, null, new Object[]{2L, 10L, null, null});
        assertEquals("page:2:10:-:-", key.toString());
    }
}

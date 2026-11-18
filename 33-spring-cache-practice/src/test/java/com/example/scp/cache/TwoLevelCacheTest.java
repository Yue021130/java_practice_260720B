package com.example.scp.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 二级缓存核心逻辑单元测试：全部 Mockito mock Redis 操作，
 * 不依赖真实 Redis / MySQL / Spring 容器，任何机器上都能跑。
 *
 * <p>验证链路：L1 命中 → L2 命中回填 L1 → 双 miss 回源 → 空值占位 → 双级驱逐。</p>
 */
class TwoLevelCacheTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private TwoLevelCache cache;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        TwoLevelCacheProperties props = new TwoLevelCacheProperties();
        props.setNullTtl(30);
        TwoLevelCacheProperties.Config product = new TwoLevelCacheProperties.Config();
        product.setTtl(600);
        product.setJitter(60);
        props.getConfigs().put("product", product);
        cache = new TwoLevelCache("product", redisTemplate, props);
    }

    @Test
    @DisplayName("L1 命中：put 后第二次 get 直接走 Caffeine，不碰 Redis")
    void l1Hit() {
        cache.put("1", "phone");
        // 第一次 get：L1 已有（put 时写入），直接命中
        Object v = cache.get("1", Object.class);
        assertEquals("phone", v);
        assertEquals(1, cache.getMetrics().snapshot().getL1Hit());
        // L2（Redis）全程不应被读
        verify(valueOps, never()).get(anyString());
    }

    @Test
    @DisplayName("L2 命中并回填 L1：L1 miss 时查 Redis，命中后回填，再次 get 变 L1 命中")
    void l2HitAndBackfill() {
        // 模拟 L1 无、Redis 有的场景
        when(valueOps.get("product::1")).thenReturn("phone");

        assertEquals("phone", cache.get("1", Object.class));
        CacheMetrics.Snapshot s1 = cache.getMetrics().snapshot();
        assertEquals(1, s1.getL2Hit());
        assertEquals(0, s1.getL1Hit());

        // 回填后第二次 get 应走 L1
        assertEquals("phone", cache.get("1", Object.class));
        assertEquals(1, cache.getMetrics().snapshot().getL1Hit());
    }

    @Test
    @DisplayName("双 miss：L1/L2 都没有时返回 null 并计入 miss")
    void doubleMiss() {
        when(valueOps.get("product::404")).thenReturn(null);
        assertNull(cache.get("404", Object.class));
        CacheMetrics.Snapshot s = cache.getMetrics().snapshot();
        assertEquals(1, s.getMiss());
        assertEquals(0, s.getL1Hit() + s.getL2Hit());
    }

    @Test
    @DisplayName("防穿透：put(null) 写入 NullValue 占位符且 TTL 只有 nullTtl(30s)")
    void nullValuePlaceholder() {
        cache.put("999", null);

        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOps).set(eq("product::999"), valueCaptor.capture(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));

        assertEquals(NullValue.INSTANCE, valueCaptor.getValue());
        assertEquals(30L, ttlCaptor.getValue(), "空值占位 TTL 必须是 nullTtl，远短于正常 TTL");

        // 命中占位符：业务视角返回 null，但不算 miss（挡住了数据库穿透）
        assertNull(cache.get("999", Object.class));
        assertEquals(0, cache.getMetrics().snapshot().getMiss());
    }

    @Test
    @DisplayName("防雪崩：正常值写入时 TTL = 基础 TTL + [0, jitter) 抖动")
    void ttlJitter() {
        cache.put("1", "phone");

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOps).set(eq("product::1"), any(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));

        long ttl = ttlCaptor.getValue();
        assertTrue(ttl >= 600 && ttl < 660,
                "TTL 应在 [600, 660) 区间内，实际 " + ttl);
    }

    @Test
    @DisplayName("evict 双清：同时失效 L1 和 Redis")
    void evictBothLevels() {
        cache.put("1", "phone");
        cache.evict("1");

        verify(redisTemplate).delete("product::1");
        // L1 已失效：get 会回退到 Redis
        when(valueOps.get("product::1")).thenReturn(null);
        assertNull(cache.get("1", Object.class));
        assertEquals(1, cache.getMetrics().snapshot().getEvict());
    }
}

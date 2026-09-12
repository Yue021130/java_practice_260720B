package com.example.sfp.interceptor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 令牌桶单元测试：验证容量限制与补充逻辑。
 */
class TokenBucketTest {

    @Test
    void tryAcquireWithinCapacity() {
        RateLimitInterceptor.TokenBucket bucket = new RateLimitInterceptor.TokenBucket(3, 1, 60000);
        assertTrue(bucket.tryAcquire());
        assertTrue(bucket.tryAcquire());
        assertTrue(bucket.tryAcquire());
        assertFalse(bucket.tryAcquire());
    }

    @Test
    void tryAcquireRefill() throws InterruptedException {
        // 容量 1，每 10ms 补充 1 个令牌
        RateLimitInterceptor.TokenBucket bucket = new RateLimitInterceptor.TokenBucket(1, 1, 10);
        assertTrue(bucket.tryAcquire());
        assertFalse(bucket.tryAcquire());
        Thread.sleep(15);
        assertTrue(bucket.tryAcquire());
    }
}

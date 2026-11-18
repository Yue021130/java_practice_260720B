package com.example.scp.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis SETNX 的简易分布式锁，用于「缓存击穿」防护演示。
 *
 * <p>热点 key 过期瞬间，大量并发同时 miss 会一起打到数据库。
 * 典型解法：只允许一个线程回源重建，其余线程等待或短暂降级。
 * 本类用 {@code SET key value NX EX seconds}（setIfAbsent 原子实现）
 * 抢锁，抢到者执行回源并写缓存，未抢到者自旋等待后读缓存。</p>
 *
 * <p>注意：这是教学版——没有看门狗续期、没有锁误删保护（value
 * 存线程标识已部分缓解）、自旋无上限。生产请直接用 Redisson 的
 * tryLock + watch dog，不要手写。</p>
 */
@Component
public class RedisLockClient {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LOCK_PREFIX = "productLock::";

    public RedisLockClient(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 双重检查 + 分布式锁的回源模板。
     *
     * @param cacheKey   业务缓存 key（如商品 id）
     * @param loader     实际回源逻辑（查库）
     * @param lockTtlSec 锁超时（秒），防止持锁线程宕机导致死锁
     */
    @SuppressWarnings("unchecked")
    public <T> T loadWithLock(String cacheKey, Supplier<T> loader, long lockTtlSec) {
        String lockKey = LOCK_PREFIX + cacheKey;
        // ① 尝试抢锁：SET NX EX，原子
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, Thread.currentThread().getId(), lockTtlSec, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                return loader.get();
            } finally {
                // ② 只删自己的锁（value 存线程 id，避免误删他人续上的锁）
                Object holder = redisTemplate.opsForValue().get(lockKey);
                if (holder != null && holder.equals(Thread.currentThread().getId())) {
                    redisTemplate.delete(lockKey);
                }
            }
        }
        // ③ 没抢到：短暂自旋等持锁者写缓存，最多 2 秒
        for (int i = 0; i < 20; i++) {
            sleepQuietly(100);
            Object cached = redisTemplate.opsForValue().get("product::" + cacheKey);
            if (cached != null && !NullValue.INSTANCE.equals(cached)) {
                return (T) cached;
            }
        }
        // ④ 等待超时兜底：直接回源（保护可用性，略压数据库）
        return loader.get();
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

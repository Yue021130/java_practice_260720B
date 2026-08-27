package com.example.caa.aspect;

import com.example.caa.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口限流切面。
 *
 * <p>基于内存计数器实现滑动窗口限流。每个方法一个窗口，窗口内请求数超过 qps 则拒绝。
 * 注意：这是教学演示，单机适用；生产环境请使用 Redis + Lua 或 Sentinel。</p>
 *
 * <p>文章提醒：RUNTIME 注解 + 反射有性能开销，高频调用场景慎用；
 * 本切面仅做教学演示，集群环境必须换分布式限流方案。</p>
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /**
     * key：方法签名，value：当前窗口的计数器和窗口开始时间。
     */
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Before("@annotation(rateLimit)")
    public void before(JoinPoint joinPoint, RateLimit rateLimit) {
        String key = getMethodKey(joinPoint);
        long now = System.currentTimeMillis();
        long windowMillis = rateLimit.timeUnit().toMillis(rateLimit.window());

        Window window = windows.compute(key, (k, old) -> {
            if (old == null || now - old.startTime > windowMillis) {
                // 窗口过期，重置
                return new Window(now, new AtomicInteger(1));
            }
            return old;
        });

        // 同一个方法并发时，只在窗口未过期时累加；如果上面重置了，这里不再重复增加
        int count;
        if (now - window.startTime <= windowMillis && window.counter.get() > 0) {
            count = window.counter.incrementAndGet();
        } else {
            count = window.counter.get();
        }

        if (count > rateLimit.qps()) {
            log.warn("[接口限流] 方法 {} 在窗口内请求 {} 次，超过 qps {}", key, count, rateLimit.qps());
            throw new IllegalStateException(rateLimit.message());
        }

        log.info("[接口限流] 方法 {} 当前窗口请求 {}/{}", key, count, rateLimit.qps());
    }

    private String getMethodKey(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }

    private static class Window {
        final long startTime;
        final AtomicInteger counter;

        Window(long startTime, AtomicInteger counter) {
            this.startTime = startTime;
            this.counter = counter;
        }
    }
}

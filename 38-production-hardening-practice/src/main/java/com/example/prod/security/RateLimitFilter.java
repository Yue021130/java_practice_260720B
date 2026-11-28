package com.example.prod.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 限流过滤器（Bucket4j 令牌桶，按 IP 维度）。
 *
 * <p>位置在 JWT 过滤器之前：限流是「流量防线」，先挡超额请求，
 * 再谈认证——被限流的请求连认证计算都不消耗。生产可换 Redis
 * 版 bucket4j 实现集群共享计数。</p>
 *
 * <p>策略：每 IP 一个桶，容量 10、每分钟匀速补充 10（可配置）。
 * 超限返回 429 + Retry-After 提示，前端据此退避。</p>
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final long capacity;
    private final long refillPerMinute;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.capacity}") long capacity,
                           @Value("${app.rate-limit.refill-per-minute}") long refillPerMinute) {
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
        }
    }

    private Bucket newBucket() {
        // 匀速补充：每分钟 refillPerMinute 个令牌，桶满即停
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refillPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}

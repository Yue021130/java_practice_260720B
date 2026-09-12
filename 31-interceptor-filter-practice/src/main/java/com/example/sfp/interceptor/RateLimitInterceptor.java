package com.example.sfp.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 限流拦截器：基于内存简易令牌桶，演示拦截器做「通用横切逻辑」。
 *
 * <p>八股：限流放在 Interceptor 的好处是——只对真正进入 Spring MVC 的请求生效，
 * 静态资源、错误页不受影响；拿到 Client IP 后按 IP 维度做令牌桶控制，
 * 超过阈值直接通过 response 写回 429，不再进入 Controller，避免浪费业务资源。</p>
 *
 * <p>策略：桶容量 10，每 6 秒补充 1 个令牌，即约 10 次/分钟；适合单机演示，
 * 分布式场景应换 Redis + Lua 原子脚本。</p>
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 每个 IP 对应一个令牌桶 */
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String ip = getClientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(10, 1, 6000));
        if (bucket.tryAcquire()) {
            return true;
        }
        log.warn("[RateLimit] IP {} 请求过于频繁：{} {}", ip, request.getMethod(), request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("msg", "请求过于频繁，请稍后再试");
        result.put("data", null);
        String body = mapper.writeValueAsString(result);
        response.getWriter().write(body);
        return false;
    }

    /** 简易令牌桶 */
    static class TokenBucket {
        /** 容量 */
        private final long capacity;
        /** 每次补充令牌数 */
        private final double refillTokens;
        /** 补充间隔（毫秒） */
        private final long refillIntervalMs;

        private double availableTokens;
        private long lastRefillTime;

        TokenBucket(long capacity, double refillTokens, long refillIntervalMs) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillIntervalMs = refillIntervalMs;
            this.availableTokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire() {
            refill();
            if (availableTokens >= 1) {
                availableTokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            if (elapsed >= refillIntervalMs) {
                long periods = elapsed / refillIntervalMs;
                availableTokens = Math.min(capacity, availableTokens + periods * refillTokens);
                lastRefillTime = now;
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_CLIENT_IP"};
        for (String h : headers) {
            String ip = request.getHeader(h);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

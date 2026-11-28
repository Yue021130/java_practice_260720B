package com.example.prod.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单：登出的 JWT 的 jti 进黑名单，TTL = Token 剩余有效期。
 *
 * <p>单机实现用 Caffeine：value 存「过期截止时间戳」，读取时判断，
 * 过期条目自然视为未拉黑且由 Caffeine 定期清理——不需要精确到毫秒的
 * 逐条 TTL，语义等价且实现简单。</p>
 *
 * <p>生产换 Redis：SET blacklist:{jti} 1 EX {remainingSeconds}，
 * 校验时用 GET + EXISTS，接口（add/contains）不变，业务代码零改动。</p>
 */
@Component
public class TokenBlacklist {

    /** 上限兜底：黑名单不可能超过签发总量，10 万足够演示 */
    private final Cache<String, Long> blacklisted = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(2, TimeUnit.DAYS)   // 兜底清理，真实 TTL 在 value 里
            .build();

    /**
     * 拉黑 jti。
     *
     * @param jti     令牌唯一 id
     * @param ttlMs   该令牌的剩余有效期；<=0 说明已过期，无需拉黑
     */
    public void add(String jti, long ttlMs) {
        if (jti == null || ttlMs <= 0) {
            return;
        }
        blacklisted.put(jti, System.currentTimeMillis() + ttlMs);
    }

    /** 是否已拉黑（且拉黑未到期） */
    public boolean contains(String jti) {
        Long expireAt = blacklisted.getIfPresent(jti);
        return expireAt != null && expireAt > System.currentTimeMillis();
    }
}

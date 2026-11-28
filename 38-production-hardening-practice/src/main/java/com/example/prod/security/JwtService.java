package com.example.prod.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与校验（jjwt 0.11 API）。
 *
 * <p>密钥从环境变量 JWT_SECRET 注入（application.yml 的 ${JWT_SECRET}），
 * 绝不硬编码——生产用 `openssl rand -hex 32` 生成长随机串。</p>
 *
 * <p>jti（JWT ID）声明：每次签发生成唯一 id，登出时把 jti 写入黑名单
 * 实现「无状态 Token 的可撤销」——这是 JWT 无状态与可撤销矛盾的
 * 标准折中。</p>
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-millis}") long ttlMillis) {
        byte[] keyBytes;
        try {
            // 支持 Base64 编码的密钥（生产推荐：JWT_SECRET 存 Base64）
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            // DecodingException 不是 IllegalArgumentException 子类，必须按 RuntimeException 兜
            // 兜底：按 UTF-8 原文处理（要求长度 >= 256bit/32字节，HS256 要求）
            keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT 密钥长度不足 32 字节（HS256 要求 256bit），请检查 JWT_SECRET");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.ttlMillis = ttlMillis;
    }

    /** 签发：subject=用户名，带 jti、签发/过期时间 */
    public String generate(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())  // jti，黑名单按它撤销
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析并校验（签名/过期），任何不合法都抛 io.jsonwebtoken.JwtException */
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    /** 从 Authorization: Bearer <token> 头中剥出 token */
    public String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 头格式应为 Bearer <token>");
        }
        return authorizationHeader.substring(7);
    }
}

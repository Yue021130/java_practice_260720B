package com.example.prod.unit;

import com.example.prod.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试（金字塔 70% 层）：JWT 签发与校验。
 */
class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef"; // 48 字节

    private final JwtService jwtService = new JwtService(SECRET, 3600_000);

    @Test
    @DisplayName("签发后可解析：subject 与 jti 正确")
    void generateAndParse() {
        String token = jwtService.generate("admin");
        Jws<Claims> jws = jwtService.parse(token);

        assertEquals("admin", jws.getBody().getSubject());
        assertNotNull(jws.getBody().getId(), "jti 必须存在（黑名单按它撤销）");
        assertTrue(jws.getBody().getExpiration().getTime() > System.currentTimeMillis());
    }

    @Test
    @DisplayName("篡改 token 校验失败")
    void tampered() {
        String token = jwtService.generate("admin");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }

    @Test
    @DisplayName("密钥不同校验失败")
    void wrongSecret() {
        String token = jwtService.generate("admin");
        JwtService other = new JwtService("abcdef0123456789abcdef0123456789abcdef0123456789", 3600_000);
        assertThrows(JwtException.class, () -> other.parse(token));
    }

    @Test
    @DisplayName("extractToken 剥离 Bearer 前缀，格式错抛异常")
    void extractToken() {
        assertEquals("abc", jwtService.extractToken("Bearer abc"));
        assertThrows(IllegalArgumentException.class, () -> jwtService.extractToken("abc"));
        assertThrows(IllegalArgumentException.class, () -> jwtService.extractToken(null));
    }
}

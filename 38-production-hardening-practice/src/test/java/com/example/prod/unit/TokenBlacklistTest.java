package com.example.prod.unit;

import com.example.prod.security.TokenBlacklist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试（金字塔 70% 层）：Token 黑名单。
 */
class TokenBlacklistTest {

    private final TokenBlacklist blacklist = new TokenBlacklist();

    @Test
    @DisplayName("拉黑后在有效期内 contains 为 true")
    void revokeAndCheck() {
        blacklist.add("jti-1", 60_000);
        assertTrue(blacklist.contains("jti-1"));
        assertFalse(blacklist.contains("jti-other"));
    }

    @Test
    @DisplayName("已过期的拉黑条目自动视为未拉黑（TTL 语义）")
    void expiredEntry() throws Exception {
        blacklist.add("jti-2", 80); // 80ms 有效
        assertTrue(blacklist.contains("jti-2"));
        Thread.sleep(120);
        assertFalse(blacklist.contains("jti-2"), "黑名单到期 = 令牌本也应过期，无需再拦");
    }

    @Test
    @DisplayName("ttl<=0 直接忽略（令牌已过期无需拉黑）")
    void nonPositiveTtlIgnored() {
        blacklist.add("jti-3", 0);
        blacklist.add("jti-4", -100);
        assertFalse(blacklist.contains("jti-3"));
        assertFalse(blacklist.contains("jti-4"));
    }
}

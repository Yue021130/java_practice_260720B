package com.example.ccp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PasswordUtil 单元测试（不依赖数据库，纯内存可跑）。
 *
 * <p>同时承担一个职责：打印 123456 的 BCrypt 哈希，用于核对 docs/sql/schema.sql 中的初始密码。</p>
 */
class PasswordUtilTest {

    private final PasswordUtil passwordUtil = new PasswordUtil();

    @Test
    void encodeAndMatch() {
        String encoded = passwordUtil.encode("123456");
        System.out.println("123456 -> " + encoded);
        assertTrue(encoded.startsWith("$2a$10$"));
        assertTrue(passwordUtil.matches("123456", encoded));
        assertFalse(passwordUtil.matches("wrong-pass", encoded));
    }

    @Test
    void sameRawProducesDifferentHash() {
        // BCrypt 每次随机加盐：相同明文，两次哈希结果不同
        assertFalse(passwordUtil.encode("123456").equals(passwordUtil.encode("123456")));
    }
}

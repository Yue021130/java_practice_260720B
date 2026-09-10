package com.example.ccp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具：基于 BCrypt 加盐哈希。
 *
 * <p>八股：为什么不能存明文/MD5？—— BCrypt 内置随机盐且支持调整强度（cost），
 * 相同明文每次哈希结果不同，可抵御彩虹表与暴力破解；MD5 是无盐快速哈希，已不安全。</p>
 */
@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 明文加密，结果形如 $2a$10$... */
    public String encode(String raw) {
        return encoder.encode(raw);
    }

    /** 校验明文与哈希是否匹配 */
    public boolean matches(String raw, String encoded) {
        return encoder.matches(raw, encoded);
    }
}

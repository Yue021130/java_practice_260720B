package com.example.satoken.jwt;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 集成与临时 Token 演示。
 *
 * Sa-Token 提供三种 JWT 模式：simple / mixin / stateless；
 * 本模块使用工具方法生成 JWT 字符串，便于在不改动全局配置的情况下学习 JWT 结构。
 */
@RestController
@RequestMapping("/api/jwt")
public class JwtController {

    private static final String JWT_SECRET = "sa-token-practice-secret-key";

    /**
     * 生成一个模拟 JWT（Header.Payload.Signature）。
     */
    @PostMapping("/generate")
    public ApiResponse<Map<String, String>> generateJwt(
            @RequestParam(defaultValue = "10001") String loginId) {
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64UrlEncode("{\"sub\":\"" + loginId + "\",\"iat\":" + System.currentTimeMillis() / 1000 + "}");
        String signature = hmacSha256(header + "." + payload, JWT_SECRET);
        String jwt = header + "." + payload + "." + signature;

        Map<String, String> data = new HashMap<>();
        data.put("jwt", jwt);
        data.put("tip", "这是教学用简化 JWT；生产环境请使用 sa-token-jwt 扩展的 StpUtil.login 生成标准 JWT");
        return ApiResponse.success(data);
    }

    /**
     * 校验模拟 JWT 签名。
     */
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verifyJwt(@RequestParam String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            return ApiResponse.error(400, "JWT 格式错误");
        }
        String expectSig = hmacSha256(parts[0] + "." + parts[1], JWT_SECRET);
        boolean valid = expectSig.equals(parts[2]);
        return ApiResponse.success(Map.of("valid", valid));
    }

    /**
     * 临时 Token 认证：生成短时效 Token（60 秒）。
     */
    @PostMapping("/temp-token")
    public ApiResponse<Map<String, Object>> tempToken(
            @RequestParam(defaultValue = "10001") Long id) {
        StpUtil.login(id, new cn.dev33.satoken.stp.SaLoginModel().setTimeout(60));
        Map<String, Object> data = new HashMap<>();
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("timeout", StpUtil.getTokenTimeout());
        data.put("tip", "临时 Token 60 秒后过期，适合邮件验证、支付确认等短时效场景");
        return ApiResponse.success(data);
    }

    /**
     * Sa-Token 三种 JWT 模式说明。
     */
    @GetMapping("/modes")
    public ApiResponse<Map<String, String>> jwtModes() {
        return ApiResponse.success(Map.of(
                "simple", "Token 本身就是 JWT，可直接解析出 loginId，但仍依赖 Redis 做校验",
                "mixin", "Token 前半段为随机字符串，后半段为 JWT，兼顾安全性与可读性",
                "stateless", "完全无 Redis 依赖，JWT 自包含所有信息，但踢人下线等功能受限"
        ));
    }

    private static String base64UrlEncode(String str) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 失败", e);
        }
    }
}

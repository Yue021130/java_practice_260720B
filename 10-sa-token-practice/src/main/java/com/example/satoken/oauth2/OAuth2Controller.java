package com.example.satoken.oauth2;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2.0 服务端与客户端交互模拟。
 *
 * 教学项目用内存结构演示四种授权模式 + 刷新令牌 + openid；
 * 真实场景请使用 Sa-Token OAuth2 扩展的 SaOAuth2Handle 与数据接口。
 */
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {

    private static final Map<String, TokenInfo> ACCESS_TOKENS = new ConcurrentHashMap<>();
    private static final Map<String, String> REFRESH_TOKENS = new ConcurrentHashMap<>();
    private static final Map<String, String> CODES = new ConcurrentHashMap<>();

    private static final long ACCESS_TOKEN_TTL = 7200;
    private static final long REFRESH_TOKEN_TTL = 86400 * 7;

    /**
     * 授权码模式 — 申请 code。
     */
    @GetMapping("/authorize")
    public ApiResponse<Map<String, String>> authorize(
            @RequestParam(defaultValue = "10001") Long id,
            @RequestParam(defaultValue = "read") String scope) {
        StpUtil.login(id);
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        CODES.put(code, id + ":" + scope);
        return ApiResponse.success(Map.of("code", code, "scope", scope));
    }

    /**
     * 授权码模式 — 用 code 换取 access_token。
     */
    @PostMapping("/token")
    public ApiResponse<Map<String, Object>> tokenByCode(@RequestParam String code) {
        String value = CODES.remove(code);
        if (value == null) {
            return ApiResponse.error(400, "授权码无效或已使用");
        }
        String[] parts = value.split(":");
        TokenInfo token = createToken(parts[0], parts[1]);
        return ApiResponse.success("授权码模式换 token 成功", token.toMap());
    }

    /**
     * 密码模式 — 用户名密码换 token。
     */
    @PostMapping("/password-token")
    public ApiResponse<Map<String, Object>> passwordToken(
            @RequestParam(defaultValue = "10001") String username,
            @RequestParam(defaultValue = "123456") String password) {
        if (!"123456".equals(password)) {
            return ApiResponse.error(400, "密码错误");
        }
        TokenInfo token = createToken(username, "read,write");
        return ApiResponse.success("密码模式换 token 成功", token.toMap());
    }

    /**
     * 客户端凭证模式。
     */
    @PostMapping("/client-token")
    public ApiResponse<Map<String, Object>> clientToken(
            @RequestParam(defaultValue = "client-app") String clientId,
            @RequestParam(defaultValue = "secret") String clientSecret) {
        if (!"secret".equals(clientSecret)) {
            return ApiResponse.error(400, "客户端密钥错误");
        }
        TokenInfo token = createToken(clientId, "app");
        return ApiResponse.success("客户端凭证模式换 token 成功", token.toMap());
    }

    /**
     * 刷新令牌。
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestParam String refreshToken) {
        String loginId = REFRESH_TOKENS.get(refreshToken);
        if (loginId == null) {
            return ApiResponse.error(400, "refresh_token 无效");
        }
        TokenInfo token = createToken(loginId, "read,write");
        return ApiResponse.success("刷新 token 成功", token.toMap());
    }

    /**
     * 受保护资源接口。
     */
    @GetMapping("/userinfo")
    public ApiResponse<Map<String, Object>> userinfo(@RequestParam String accessToken) {
        TokenInfo info = ACCESS_TOKENS.get(accessToken);
        if (info == null) {
            return ApiResponse.error(401, "access_token 无效");
        }
        return ApiResponse.success(Map.of(
                "loginId", info.loginId,
                "scope", info.scope,
                "nickname", "User-" + info.loginId
        ));
    }

    /**
     * openid 模式。
     */
    @GetMapping("/openid")
    public ApiResponse<Map<String, String>> openid(@RequestParam String accessToken) {
        TokenInfo info = ACCESS_TOKENS.get(accessToken);
        if (info == null) {
            return ApiResponse.error(401, "access_token 无效");
        }
        String openid = UUID.nameUUIDFromBytes(info.loginId.getBytes()).toString().replace("-", "");
        return ApiResponse.success(Map.of("openid", openid, "loginId", info.loginId));
    }

    private TokenInfo createToken(String loginId, String scope) {
        String accessToken = "at-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String refreshToken = "rt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        TokenInfo info = new TokenInfo(loginId, scope, accessToken, refreshToken, ACCESS_TOKEN_TTL, REFRESH_TOKEN_TTL);
        ACCESS_TOKENS.put(accessToken, info);
        REFRESH_TOKENS.put(refreshToken, loginId);
        return info;
    }

    static class TokenInfo {
        String loginId;
        String scope;
        String accessToken;
        String refreshToken;
        long accessTokenExpire;
        long refreshTokenExpire;

        TokenInfo(String loginId, String scope, String accessToken, String refreshToken, long accessTokenExpire, long refreshTokenExpire) {
            this.loginId = loginId;
            this.scope = scope;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessTokenExpire = accessTokenExpire;
            this.refreshTokenExpire = refreshTokenExpire;
        }

        Map<String, Object> toMap() {
            return Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "tokenType", "Bearer",
                    "expiresIn", accessTokenExpire,
                    "scope", scope,
                    "loginId", loginId
            );
        }
    }
}

package com.example.satoken.sso;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSO 单点登录模拟。
 *
 * 教学项目在同一应用内模拟服务端 + 两个客户端的交互；
 * 真实场景下服务端与客户端部署在不同域名，需配合 Sa-Token SSO 扩展与 Cookie 共享策略。
 */
@RestController
@RequestMapping("/api/sso")
public class SsoController {

    /** 模拟 SSO 服务端全局登录态：ssoToken -> loginId */
    private static final Map<String, Object> SSO_SERVER_SESSION = new ConcurrentHashMap<>();

    /** 模拟客户端登录态：clientToken -> ssoToken */
    private static final Map<String, String> CLIENT_SESSION = new ConcurrentHashMap<>();

    /**
     * SSO 服务端登录。
     */
    @PostMapping("/do-login")
    public ApiResponse<Map<String, String>> ssoLogin(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.login(id);
        String ssoToken = UUID.randomUUID().toString().replace("-", "");
        SSO_SERVER_SESSION.put(ssoToken, id);

        // 同时给客户端 1 和客户端 2 颁发子系统 Token
        String client1Token = "client1-" + UUID.randomUUID().toString().substring(0, 8);
        String client2Token = "client2-" + UUID.randomUUID().toString().substring(0, 8);
        CLIENT_SESSION.put(client1Token, ssoToken);
        CLIENT_SESSION.put(client2Token, ssoToken);

        Map<String, String> data = Map.of(
                "ssoToken", ssoToken,
                "client1Token", client1Token,
                "client2Token", client2Token,
                "loginId", String.valueOf(id)
        );
        return ApiResponse.success("SSO 服务端登录成功", data);
    }

    /**
     * 查询 SSO 服务端全局登录态。
     */
    @GetMapping("/is-login")
    public ApiResponse<Map<String, Object>> ssoIsLogin(@RequestParam String ssoToken) {
        Object loginId = SSO_SERVER_SESSION.get(ssoToken);
        boolean isLogin = loginId != null;
        return ApiResponse.success(Map.of(
                "isLogin", isLogin,
                "loginId", isLogin ? loginId : "none"
        ));
    }

    /**
     * 客户端 1 校验登录态。
     */
    @GetMapping("/client1/info")
    public ApiResponse<Map<String, Object>> client1Info(@RequestParam String client1Token) {
        String ssoToken = CLIENT_SESSION.get(client1Token);
        Object loginId = ssoToken == null ? null : SSO_SERVER_SESSION.get(ssoToken);
        boolean isLogin = loginId != null;
        return ApiResponse.success(Map.of(
                "client", "client1",
                "isLogin", isLogin,
                "loginId", isLogin ? loginId : "none"
        ));
    }

    /**
     * 客户端 2 校验登录态。
     */
    @GetMapping("/client2/info")
    public ApiResponse<Map<String, Object>> client2Info(@RequestParam String client2Token) {
        String ssoToken = CLIENT_SESSION.get(client2Token);
        Object loginId = ssoToken == null ? null : SSO_SERVER_SESSION.get(ssoToken);
        boolean isLogin = loginId != null;
        return ApiResponse.success(Map.of(
                "client", "client2",
                "isLogin", isLogin,
                "loginId", isLogin ? loginId : "none"
        ));
    }

    /**
     * SSO 单点注销。
     */
    @SaIgnore
    @PostMapping("/logout")
    public ApiResponse<Void> ssoLogout(@RequestParam String ssoToken) {
        SSO_SERVER_SESSION.remove(ssoToken);
        CLIENT_SESSION.values().removeIf(v -> v.equals(ssoToken));
        StpUtil.logout();
        return ApiResponse.success("SSO 单点注销成功", null);
    }

    /**
     * 公开说明：SSO 三种模式速查。
     */
    @GetMapping("/public/modes")
    public ApiResponse<Map<String, String>> ssoModes() {
        return ApiResponse.success(Map.of(
                "同域模式", "所有系统在同一主域名下，Cookie 可共享，配置最简单",
                "跨域模式", "系统域名不同，需通过 Sa-Token ticket 机制在跳转时传递登录态",
                "跨 Redis 模式", "服务端与客户端不共享 Redis，需调用 http 接口校验登录态",
                "本模块", "使用内存 Map 模拟服务端/客户端交互，便于本地学习"
        ));
    }
}

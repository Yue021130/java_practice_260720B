package com.example.satoken.login;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录认证：登录 / 注销 / 查询登录状态 / Token 操作。
 */
@RestController
@RequestMapping("/api/login")
public class LoginController {

    /**
     * 登录：指定账号 id 登录。
     */
    @PostMapping("/do-login")
    public ApiResponse<Map<String, Object>> doLogin(@RequestParam(defaultValue = "10001") Long id) {
        // 先校验账号是否被封禁（演示业务层主动检查）
        StpUtil.checkDisable(id);
        StpUtil.login(id);
        Map<String, Object> data = buildLoginInfo();
        return ApiResponse.success("登录成功", data);
    }

    /**
     * 注销当前会话。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.success("注销成功", null);
    }

    /**
     * 当前是否已登录。
     */
    @GetMapping("/is-login")
    public ApiResponse<Boolean> isLogin() {
        return ApiResponse.success(StpUtil.isLogin());
    }

    /**
     * 获取当前 Token 值。
     */
    @GetMapping("/token-value")
    public ApiResponse<String> tokenValue() {
        return ApiResponse.success(StpUtil.getTokenValue());
    }

    /**
     * 获取当前登录账号 id。
     */
    @GetMapping("/login-id")
    public ApiResponse<Object> loginId() {
        return ApiResponse.success(StpUtil.getLoginIdDefaultNull());
    }

    /**
     * 使用指定 Token 登录（常用于单点登录、前后端分离场景）。
     */
    @PostMapping("/login-by-token")
    public ApiResponse<Map<String, Object>> loginByToken(@RequestParam String tokenValue) {
        StpUtil.setTokenValue(tokenValue);
        Map<String, Object> data = buildLoginInfo();
        return ApiResponse.success("指定 Token 登录成功", data);
    }

    private Map<String, Object> buildLoginInfo() {
        Map<String, Object> map = new HashMap<>();
        map.put("tokenValue", StpUtil.getTokenValue());
        map.put("loginId", StpUtil.getLoginId());
        map.put("tokenName", StpUtil.getTokenName());
        map.put("timeout", StpUtil.getTokenTimeout());
        return map;
    }
}

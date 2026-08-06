package com.example.satoken.session;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Session 会话：Account-Session / Token-Session / 自定义 Session / 会话查询。
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    private static final String CUSTOM_SESSION_ID = "custom:biz";

    /**
     * 在 Account-Session 中写入数据。
     */
    @PostMapping("/account/set")
    public ApiResponse<Void> accountSessionSet(@RequestParam String key, @RequestParam Object value) {
        StpUtil.getSession().set(key, value);
        return ApiResponse.success("Account-Session 写入成功", null);
    }

    /**
     * 从 Account-Session 中读取数据。
     */
    @GetMapping("/account/get")
    public ApiResponse<Object> accountSessionGet(@RequestParam String key) {
        return ApiResponse.success(StpUtil.getSession().get(key));
    }

    /**
     * 在 Token-Session 中写入数据。
     */
    @PostMapping("/token/set")
    public ApiResponse<Void> tokenSessionSet(@RequestParam String key, @RequestParam Object value) {
        StpUtil.getTokenSession().set(key, value);
        return ApiResponse.success("Token-Session 写入成功", null);
    }

    /**
     * 从 Token-Session 中读取数据。
     */
    @GetMapping("/token/get")
    public ApiResponse<Object> tokenSessionGet(@RequestParam String key) {
        return ApiResponse.success(StpUtil.getTokenSession().get(key));
    }

    /**
     * 自定义 Session 读写。
     */
    @PostMapping("/custom/set")
    public ApiResponse<Void> customSessionSet(@RequestParam String key, @RequestParam Object value) {
        SaSession session = StpUtil.getSessionBySessionId(CUSTOM_SESSION_ID);
        session.set(key, value);
        return ApiResponse.success("自定义 Session 写入成功", null);
    }

    @GetMapping("/custom/get")
    public ApiResponse<Object> customSessionGet(@RequestParam String key) {
        SaSession session = StpUtil.getSessionBySessionId(CUSTOM_SESSION_ID);
        return ApiResponse.success(session.get(key));
    }

    /**
     * 查询当前账号的所有登录会话。
     */
    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> searchSessions() {
        List<String> tokenValues = StpUtil.getTokenValueListByLoginId(StpUtil.getLoginId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (String token : tokenValues) {
            Map<String, Object> item = new HashMap<>();
            item.put("tokenValue", token);
            item.put("loginId", StpUtil.getLoginId());
            item.put("tokenTimeout", StpUtil.getTokenTimeout());
            list.add(item);
        }
        return ApiResponse.success(list);
    }

    /**
     * 当前账号登录设备数。
     */
    @GetMapping("/login-device-count")
    public ApiResponse<Integer> loginDeviceCount() {
        return ApiResponse.success(StpUtil.getTokenValueListByLoginId(StpUtil.getLoginId()).size());
    }
}

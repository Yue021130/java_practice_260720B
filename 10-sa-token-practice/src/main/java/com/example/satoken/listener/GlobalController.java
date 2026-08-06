package com.example.satoken.listener;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局侦听器 / 过滤器测试接口。
 */
@RestController
@RequestMapping("/api/global")
public class GlobalController {

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.login(id);
        Map<String, Object> data = new HashMap<>();
        data.put("tokenValue", StpUtil.getTokenValue());
        return ApiResponse.success("登录成功，请观察控制台监听器日志", data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.success("注销成功，请观察控制台监听器日志", null);
    }

    @GetMapping("/filter-test")
    public ApiResponse<String> filterTest() {
        return ApiResponse.success("全局过滤器已生效（响应头 X-Sa-Token-Practice）");
    }
}

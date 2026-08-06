package com.example.satoken.loginmodel;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录模型：单端 / 多端 / 同端互斥 / 记住我 / 七天免登。
 *
 * Sa-Token 1.39 中，并发登录/同端互斥通过全局配置 is-concurrent / is-share 控制；
 * 本控制器使用 device + timeout 演示多端与记住我场景，并发策略通过注释说明。
 */
@RestController
@RequestMapping("/api/login-model")
public class LoginModelController {

    /**
     * 单端登录：同一账号仅允许一个设备在线，新登录挤掉旧登录。
     *
     * 实际配置：sa-token.is-concurrent=false, sa-token.is-share=true
     */
    @PostMapping("/single")
    public ApiResponse<Map<String, Object>> singleDeviceLogin(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.login(id, new SaLoginModel().setDevice("PC"));
        return ApiResponse.success("单端登录成功（同账号仅一个设备在线，需配合 is-concurrent=false）", buildInfo("single"));
    }

    /**
     * 多端登录：同一账号多个设备同时在线。
     *
     * 实际配置：sa-token.is-concurrent=true, sa-token.is-share=false
     */
    @PostMapping("/multi")
    public ApiResponse<Map<String, Object>> multiDeviceLogin(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.login(id, new SaLoginModel().setDevice("PC"));
        return ApiResponse.success("多端登录成功（PC 端，需配合 is-concurrent=true & is-share=false）", buildInfo("multi-pc"));
    }

    /**
     * 同端互斥：相同端类型互斥（例如两个手机不能同时在线），但 PC 与手机可同时在线。
     *
     * 实际配置：sa-token.is-concurrent=true, sa-token.max-login-count=按端控制
     */
    @PostMapping("/mutex")
    public ApiResponse<Map<String, Object>> mutexDeviceLogin(
            @RequestParam(defaultValue = "10001") Long id,
            @RequestParam(defaultValue = "phone") String device) {
        StpUtil.login(id, new SaLoginModel().setDevice(device));
        return ApiResponse.success("同端互斥登录成功（device=" + device + "）", buildInfo("mutex-" + device));
    }

    /**
     * 记住我：长时效 Token（默认 30 天）。
     */
    @PostMapping("/remember")
    public ApiResponse<Map<String, Object>> rememberMe(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.login(id, new SaLoginModel()
                .setDevice("PC")
                .setTimeout(60 * 60 * 24 * 30)); // 30 天
        return ApiResponse.success("记住我登录成功（Token 30 天有效）", buildInfo("remember"));
    }

    /**
     * 七天免登：Token 有效期 7 天。
     */
    @PostMapping("/7days")
    public ApiResponse<Map<String, Object>> sevenDaysLogin(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.login(id, new SaLoginModel()
                .setDevice("PC")
                .setTimeout(60 * 60 * 24 * 7)); // 7 天
        return ApiResponse.success("七天免登登录成功", buildInfo("7days"));
    }

    private Map<String, Object> buildInfo(String model) {
        Map<String, Object> map = new HashMap<>();
        map.put("model", model);
        map.put("tokenValue", StpUtil.getTokenValue());
        map.put("loginId", StpUtil.getLoginId());
        map.put("tokenTimeout", StpUtil.getTokenTimeout());
        map.put("loginDeviceCount", StpUtil.getTokenValueListByLoginId(StpUtil.getLoginId()).size());
        return map;
    }
}

package com.example.satoken.gateway;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 网关 / 微服务鉴权思路演示。
 *
 * 在 Gateway / ShenYu / Zuul 等网关中，通常只校验登录态；
 * 具体权限校验下沉到各微服务或统一权限中心。
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    /**
     * 模拟 Gateway 登录态校验：从 Header 取出 Token 判断是否登录。
     */
    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> gatewayCheck() {
        StpUtil.checkLogin();
        return ApiResponse.success(Map.of(
                "gateway", "passed",
                "loginId", StpUtil.getLoginId(),
                "route", "转发到下游服务",
                "tip", "网关层只负责 Token 是否有效；具体权限由下游服务自行判断"
        ));
    }

    /**
     * 网关鉴权说明。
     */
    @GetMapping("/intro")
    public ApiResponse<Map<String, String>> gatewayIntro() {
        return ApiResponse.success(Map.of(
                "Gateway", "Spring Cloud Gateway + Sa-Reactor-Filter 校验登录态",
                "ShenYu", "通过 ShenYu 插件从 Header 读取 Token 并调用 Sa-Token 校验",
                "Zuul", "Zuul Filter 中调用 StpUtil.getTokenValue() / checkLogin()",
                "微服务间", "通过 Feign / Dubbo 拦截器把登录态透传到下游"
        ));
    }
}

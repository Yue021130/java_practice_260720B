package com.example.satoken.integration;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.SaManager;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 集成扩展：Redis 持久化、前后端分离、Token 风格、自动续签。
 */
@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    /**
     * 检查当前 Sa-Token 持久化实现。
     */
    @GetMapping("/dao-type")
    public ApiResponse<Map<String, String>> daoType() {
        SaTokenDao dao = SaManager.getSaTokenDao();
        Map<String, String> data = new HashMap<>();
        data.put("daoClass", dao.getClass().getName());
        data.put("isMemory", String.valueOf(dao instanceof SaTokenDaoDefaultImpl));
        data.put("isRedis", String.valueOf(dao instanceof SaTokenDaoRedisJackson));
        data.put("note", "取消 application.yml 中 redis 注释并启动本地 Redis 后，Sa-Token 将自动切换为 Redis 持久化");
        return ApiResponse.success(data);
    }

    /**
     * 前后端分离：使用 Header 提交 Token 登录（关闭持久 Cookie，仅本次响应携带临时 Cookie）。
     */
    @PostMapping("/header-token")
    public ApiResponse<Map<String, Object>> headerTokenLogin(
            @RequestParam(defaultValue = "10001") Long id,
            @RequestParam(defaultValue = "false") boolean closeCookie) {
        SaLoginModel model = new SaLoginModel().setIsLastingCookie(!closeCookie);
        StpUtil.login(id, model);
        Map<String, Object> data = new HashMap<>();
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("tokenName", StpUtil.getTokenName());
        data.put("closeCookie", closeCookie);
        data.put("tip", "关闭 Cookie 后，前端请在请求头中携带：satoken=" + StpUtil.getTokenValue());
        return ApiResponse.success("前后端分离登录成功", data);
    }

    /**
     * 查看当前 Token 有效期。
     */
    @GetMapping("/token-timeout")
    public ApiResponse<Map<String, Long>> tokenTimeout() {
        Map<String, Long> data = new HashMap<>();
        data.put("tokenTimeout", StpUtil.getTokenTimeout());
        data.put("sessionTimeout", StpUtil.getSessionTimeout());
        return ApiResponse.success(data);
    }

    /**
     * 手动续签 Token。
     */
    @PostMapping("/renew")
    public ApiResponse<Map<String, Long>> renew() {
        long before = StpUtil.getTokenTimeout();
        StpUtil.renewTimeout(60 * 60 * 24 * 7); // 续签 7 天
        long after = StpUtil.getTokenTimeout();
        Map<String, Long> data = new HashMap<>();
        data.put("before", before);
        data.put("after", after);
        return ApiResponse.success("Token 已续签", data);
    }

    /**
     * 公开接口：用于测试跨域 / 过滤器放行。
     */
    @GetMapping("/public/info")
    public ApiResponse<String> publicInfo() {
        return ApiResponse.success("公开接口，无需登录");
    }
}

package com.example.satoken.permission;

import cn.dev33.satoken.annotation.SaIgnore;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 路由拦截鉴权演示。
 *
 * 拦截规则在 {@link com.example.satoken.config.SaTokenInterceptorConfig} 中配置。
 */
@RestController
@RequestMapping("/api/route")
public class RouteController {

    @GetMapping("/user/info")
    public ApiResponse<String> userInfo() {
        return ApiResponse.success("用户模块信息");
    }

    @GetMapping("/admin/info")
    public ApiResponse<String> adminInfo() {
        return ApiResponse.success("管理员模块信息");
    }

    @SaIgnore
    @GetMapping("/public/info")
    public ApiResponse<String> publicInfo() {
        return ApiResponse.success("公开接口，无需登录");
    }

    @GetMapping("/res/{id}")
    public ApiResponse<String> resGet(@PathVariable Long id) {
        return ApiResponse.success("GET 查询资源：" + id);
    }

    @PostMapping("/res/{id}")
    public ApiResponse<String> resPost(@PathVariable Long id) {
        return ApiResponse.success("POST 修改资源：" + id);
    }

    @DeleteMapping("/res/{id}")
    public ApiResponse<String> resDelete(@PathVariable Long id) {
        return ApiResponse.success("DELETE 删除资源：" + id);
    }
}

package com.example.satoken.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由拦截器注册。
 *
 * 本配置演示基于路径的权限控制：
 * 1. 对公开接口直接放行；
 * 2. 对需要登录的接口统一校验登录；
 * 3. 对 user / admin / RESTful 资源按路径或方法鉴权。
 */
@Configuration
public class SaTokenInterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 公开接口直接放行
            SaRouter.match("/**")
                    .notMatch(
                            // 登录与登录模型
                            "/api/login/**",
                            "/api/login-model/**",
                            // 全局监听/过滤测试
                            "/api/global/**",
                            // 集成扩展（公开 + 登录接口）
                            "/api/integration/public/**",
                            "/api/integration/dao-type",
                            "/api/integration/header-token",
                            "/api/integration/token-timeout",
                            // SSO（do-login/is-login/client-info 公开）
                            "/api/sso/do-login",
                            "/api/sso/is-login",
                            "/api/sso/client1/info",
                            "/api/sso/client2/info",
                            "/api/sso/public/**",
                            // OAuth2
                            "/api/oauth2/**",
                            // JWT / 签名 / 网关 / RPC / Quick
                            "/api/jwt/**",
                            "/api/signature/**",
                            "/api/gateway/**",
                            "/api/rpc/**",
                            "/api/quick/**",
                            // 权限模块里的登录并写入权限接口
                            "/api/permission/login-with-perms",
                            // Swagger / 静态资源
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/webjars/**",
                            "/",
                            "/index.html",
                            "/assets/**")
                    .check(r -> StpUtil.checkLogin());

            // user 模块需要 user 权限
            SaRouter.match("/api/route/user/**").check(r -> StpUtil.checkPermission("user"));
            // admin 模块需要 admin 权限
            SaRouter.match("/api/route/admin/**").check(r -> StpUtil.checkPermission("admin"));
            // RESTful 风格接口按请求方法鉴权
            SaRouter.match("/api/route/res/**", "GET").check(r -> StpUtil.checkPermission("res:read"));
            SaRouter.match("/api/route/res/**", "POST").check(r -> StpUtil.checkPermission("res:write"));
            SaRouter.match("/api/route/res/**", "DELETE").check(r -> StpUtil.checkPermission("res:delete"));
        })).addPathPatterns("/api/**");
    }
}

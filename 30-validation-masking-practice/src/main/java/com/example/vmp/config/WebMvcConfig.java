package com.example.vmp.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 Sa-Token 拦截器：统一校验登录态。
 *
 * <p>拦截 /api/** 下所有请求，仅放行 /api/auth/login（登录接口不能要求登录）。
 * 未携带或携带失效 JWT 的请求在这里抛出 NotLoginException，由全局异常处理返回 401。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler ->
                        SaRouter.match("/api/**")
                                .notMatch("/api/auth/login")
                                .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/api/**");
    }
}

package com.example.sfp.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.example.sfp.interceptor.LogInterceptor;
import com.example.sfp.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 注册 Spring MVC Interceptor：按顺序拼接限流、登录校验、操作审计三个横切能力。
 *
 * <p>执行顺序（preHandle 正向，afterCompletion 反向）：
 * RateLimit -> Sa-Token -> Log。
 * 因此限流最先挡住流量，登录校验其次，最后由审计拦截器记录耗时、用户、状态码。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LogInterceptor logInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. IP 限流拦截器：对 /api/** 生效，登录接口也限流防止暴力破解
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/**");

        // 2. Sa-Token 登录校验：统一校验登录态
        registry.addInterceptor(new SaInterceptor(handler ->
                        SaRouter.match("/api/**")
                                .notMatch("/api/auth/login")
                                .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/api/**");

        // 3. 操作审计拦截器：记录请求日志
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/api/**");
    }
}

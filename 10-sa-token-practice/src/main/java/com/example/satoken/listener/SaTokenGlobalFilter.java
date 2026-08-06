package com.example.satoken.listener;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 全局过滤器配置。
 *
 * 演示在过滤器层统一设置响应头、跨域、请求耗时等。
 */
@Configuration
public class SaTokenGlobalFilter {

    @Bean
    public SaServletFilter saServletFilter() {
        return new SaServletFilter()
                // 拦截所有路由
                .addInclude("/**")
                // 放行静态资源与 Swagger
                .addExclude("/favicon.ico", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                // 前置函数：设置响应头
                .setBeforeAuth(obj -> {
                    SaHolder.getResponse().setHeader("X-Sa-Token-Practice", "enabled");
                    SaHolder.getResponse().setHeader("Access-Control-Expose-Headers", "X-Sa-Token-Practice");
                })
                // 异常处理函数
                .setError(e -> cn.dev33.satoken.util.SaResult.error(e.getMessage()));
    }
}

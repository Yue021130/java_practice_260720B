package com.example.sign.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册签名鉴权拦截器。
 *
 * 拦截器拦截全部 /** 路径，但 preHandle 内部只对标注 {@link RequireSign}
 * 的接口做校验（见 SignAuthInterceptor），因此演示接口与静态资源不受影响，
 * 被保护的接口自动走「取头 → 时间戳 → nonce → appkey → 验签」链路。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SignAuthInterceptor signAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signAuthInterceptor).addPathPatterns("/**");
    }
}

package com.example.ccp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j（OpenAPI3）文档配置：启动后访问 http://localhost:8080/doc.html 查看接口文档并在线调试。
 *
 * <p>在线调试注意：除 /api/auth/login 外均需登录，可先在登录接口拿到 token，
 * 然后在 Knife4j 右上角「Authorize」中填入 token 值（Sa-Token 默认从 Authorization 请求头读取）。</p>
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI ccpOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("第29章：Sa-Token JWT 无状态登录 + Caffeine 缓存")
                .description("Vue3 + SpringBoot 前后端分离实战接口文档")
                .version("1.0.0"));
    }
}

package com.example.rbac.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 配置：使用 JWT Simple 模式，实现无状态登录
 * （token 本身携带 loginId，无需服务端存储会话）
 * JWT 密钥在 application.yml 的 sa-token.jwt-secret-key 中配置
 */
@Configuration
public class SaTokenConfig {

    @Bean
    public StpLogic getStpLogicJwt() {
        // 指定 StpLogic 为 JWT 风格
        return new StpLogicJwtForSimple();
    }
}

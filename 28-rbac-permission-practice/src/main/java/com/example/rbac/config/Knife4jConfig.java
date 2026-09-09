package com.example.rbac.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 接口文档配置
 *
 * <p>访问地址：http://localhost:8080/doc.html</p>
 * <p>调试方式：文档右上角「全局参数」中设置 Authorization 为登录接口返回的 token</p>
 */
@Configuration
public class Knife4jConfig {

    private static final String SECURITY_SCHEME_NAME = "Authorization";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("第28章：RBAC 权限 + 动态路由 + 数据权限实战")
                        .description("Sa-Token JWT 无状态登录 + RBAC 权限点鉴权 + MyBatis-Plus 数据权限拦截器")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("登录接口返回的 token，直接粘贴即可")));
    }
}

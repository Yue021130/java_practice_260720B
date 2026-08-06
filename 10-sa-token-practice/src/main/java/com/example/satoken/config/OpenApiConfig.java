package com.example.satoken.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置。
 *
 * 启动后访问：
 * - Swagger UI 页面：http://localhost:8090/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8090/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI saTokenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sa-Token 全功能实践 API")
                        .description("Sa-Token 学习项目的实验接口：登录认证 / 权限鉴权 / Session / 踢人封禁 / "
                                + "SSO / OAuth2.0 / Redis / JWT 等全部主要能力，全部包装成可运行的现实业务场景。"
                                + "配合前端面板（http://localhost:5183）观察鉴权行为。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 10-sa-token-practice")));
    }
}

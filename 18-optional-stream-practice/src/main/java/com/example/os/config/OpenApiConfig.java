package com.example.os.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置：生成 Swagger UI 文档。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Optional + Stream 真实业务场景实践")
                        .description("Java Optional 与 Stream 结合在 Spring Boot 中的工程化落地")
                        .version("1.0.0"));
    }
}

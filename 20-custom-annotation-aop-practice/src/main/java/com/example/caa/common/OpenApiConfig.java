package com.example.caa.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("自定义注解 + AOP 高阶玩法实战")
                        .description("Spring Boot 自定义注解与 AOP 切面完整落地")
                        .version("1.0.0"));
    }
}

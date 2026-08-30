package com.example.eep.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Excel 导入导出实战接口")
                        .version("0.0.1")
                        .description("Easypoi 导入校验 + EasyExcel 自定义 Converter"));
    }
}

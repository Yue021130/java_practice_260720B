package com.example.ur.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置：生成 Swagger UI 文档。
 *
 * <p>说明：本模块所有接口默认都会被 {@link com.example.ur.common.advice.GlobalResponseAdvice}
 * 包装成 {@link com.example.ur.common.result.Result} 结构，即实际返回为 Result&lt;T&gt;。
 * 接口文档里 Controller 方法上已通过 @Operation 描述 data 字段的真实类型。</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot 统一返回结果封装实战")
                        .description("所有接口默认返回 Result&lt;T&gt; 结构：code / msg / data / timestamp")
                        .version("1.0.0"));
    }
}

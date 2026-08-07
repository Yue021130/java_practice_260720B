package com.example.threadpooladvanced.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("11-thread-pool-advanced-practice 接口文档")
                        .description("Java 线程池深度实践：源码分析 / 阻塞队列 / 拒绝策略 / Executors 风险 / 生命周期")
                        .version("1.0.0"));
    }
}

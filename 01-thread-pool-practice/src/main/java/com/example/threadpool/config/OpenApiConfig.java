package com.example.threadpool.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置。
 *
 * 引入 springdoc-openapi-ui 后，框架会自动扫描 @RestController 及
 * swagger 注解生成接口文档，无需额外代码；这里只定制文档的
 * 标题、描述、版本等元信息。
 *
 * 启动后访问：
 * - Swagger UI 页面：http://localhost:8081/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI threadPoolOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java 线程池实践 API")
                        .description("线程池学习项目的实验接口：提交压测任务、查看实时指标、动态调整参数。"
                                + "配合前端监控面板（http://localhost:5174）观察线程池七大参数与拒绝策略的效果。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 01-thread-pool-practice")));
    }
}

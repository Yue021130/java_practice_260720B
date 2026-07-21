package com.example.juc.config;

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
 * - Swagger UI 页面：http://localhost:8082/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8082/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jucOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java 并发包（JUC）全场景实践 API")
                        .description("JUC 学习项目的实验接口：锁 / 原子类 / 并发容器 / 同步工具 / "
                                + "CompletableFuture / ThreadLocal / JMM 基础，全部包装成可运行的现实业务场景。"
                                + "配合前端面板（http://localhost:5175）观察多线程行为时间线。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 02-juc-practice")));
    }
}

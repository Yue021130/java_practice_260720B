package com.example.cache.config;

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
 * - Swagger UI 页面：http://localhost:8095/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8095/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cacheOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot + Caffeine 缓存实践 API")
                        .description("Spring Boot + Caffeine 学习项目的实验接口：快速开始 / 淘汰策略 / 刷新与异步 / "
                                + "统计监控 / 缓存预热 / 穿透击穿雪崩 / 两级缓存 / Spring Cache 注解 / 缓存一致性 / "
                                + "常见坑与调优，全部包装成可运行的现实业务场景。"
                                + "配合前端面板（http://localhost:5188）观察缓存命中过程。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 15-caffeine-practice")));
    }
}

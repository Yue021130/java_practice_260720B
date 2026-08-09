package com.example.unsafe.config;

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
 * - Swagger UI 页面：http://localhost:8093/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8093/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI unsafeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("魔法类 Unsafe 实践 API")
                        .description("sun.misc.Unsafe（魔法类）学习项目的实验接口：初识与获取 / 堆外内存 / 绕过构造器 / CAS 原子操作 / "
                                + "字段偏移与对象布局 / park-unpark / 内存屏障 / 危险与本质，"
                                + "全部包装成可运行的底层实验场景。配合前端面板（http://localhost:5186）观察运行结果。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 13-unsafe-practice")));
    }
}

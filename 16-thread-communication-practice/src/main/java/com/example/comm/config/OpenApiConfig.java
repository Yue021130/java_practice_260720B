package com.example.comm.config;

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
 * - Swagger UI 页面：http://localhost:8096/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8096/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI commOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java 线程间通信方式实践 API")
                        .description("Java 线程间通信学习项目的实验接口：共享内存(volatile/原子类) / 等待通知(wait-notify/Condition) / "
                                + "线程协作控制(join/interrupt/LockSupport) / JUC 同步工具 / 阻塞队列 / 异步结果传递 / "
                                + "管道与其他通道 / 选型总结，全部包装成可运行的现实业务场景。"
                                + "配合前端面板（http://localhost:5189）观察线程通信过程。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 16-thread-communication-practice")));
    }
}

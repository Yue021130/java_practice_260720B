package com.example.mail.config;

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
 * - Swagger UI 页面：http://localhost:8092/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8092/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mailOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot 邮件服务实践 API")
                        .description("Spring Boot 邮件服务学习项目的实验接口：基础文本 / 富文本 / 附件 / 内联图片 / "
                                + "Thymeleaf 模板 / 异步发送 / 失败重试 / 定时批量 / 编码与邮件头 / 常见坑，"
                                + "全部包装成可运行的现实业务场景。配合前端面板（http://localhost:5185）观察发送过程。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 12-springboot-mail-practice")));
    }
}

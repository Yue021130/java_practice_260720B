package com.example.sign.config;

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
 * - Swagger UI 页面：http://localhost:8097/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8097/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI signOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HMAC-SHA256 接口签名鉴权实践 API")
                        .description("基于 appid + appkey 的接口签名鉴权学习项目的实验接口：核心原理 / 签名计算 / 服务端验签 / "
                                + "防重放(时间戳+nonce) / 请求体完整性 / 规范化 / 简化版 / 拦截器实战 / 选型对比，"
                                + "全部包装成可运行的现实场景。"
                                + "配合前端面板（http://localhost:5190）观察签名校验过程。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 17-api-signature-practice")));
    }
}

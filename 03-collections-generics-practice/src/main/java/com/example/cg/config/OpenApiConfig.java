package com.example.cg.config;

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
 * - Swagger UI 页面：http://localhost:8083/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8083/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI collectionsGenericsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java 集合与泛型全场景实践 API")
                        .description("Java 集合与泛型学习项目的实验接口：List / Set / Map / Queue / "
                                + "Collections / Arrays / 泛型 PECS / 类型擦除 / 泛型方法 / 综合实战，"
                                + "全部包装成可运行的现实业务场景。"
                                + "配合前端面板（http://localhost:5176）观察实验结果。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 03-collections-generics-practice")));
    }
}

package com.example.mp.config;

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
 * - Swagger UI 页面：http://localhost:8084/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8084/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mybatisPlusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot + MyBatis-Plus 全场景实践 API")
                        .description("Spring Boot + MyBatis-Plus 学习项目的实验接口：实体注解 / BaseMapper / IService / "
                                + "条件构造器 / 分页 / 逻辑删除 / 乐观锁 / 自动填充 / 批量操作 / 综合实战，"
                                + "全部包装成可运行的现实业务场景。"
                                + "配合前端面板（http://localhost:5177）观察实验结果。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 04-mybatis-plus-practice")));
    }
}

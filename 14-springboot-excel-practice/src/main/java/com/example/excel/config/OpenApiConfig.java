package com.example.excel.config;

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
 * - Swagger UI 页面：http://localhost:8094/swagger-ui/index.html
 * - OpenAPI JSON：  http://localhost:8094/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI excelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot + EasyExcel 导入导出实践 API")
                        .description("Spring Boot + EasyExcel 学习项目的实验接口：注解映射 / 样式 / 复杂表头 / 大数据量导出 / "
                                + "数据校验与错误回写 / 监听器增量读取 / 模板填充 / Web 下载与上传 / 常见坑与调优，"
                                + "全部包装成可运行的现实业务场景。配合前端面板（http://localhost:5187）观察读写过程。")
                        .version("v0.0.1")
                        .contact(new Contact().name("java高级知识 - 14-springboot-excel-practice")));
    }
}

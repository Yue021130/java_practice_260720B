package com.example.ts.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 接口文档配置
 *
 * <p>访问地址：http://localhost:8080/doc.html</p>
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("第26章：树形结构与策略模式实战")
                        .description("Hutool TreeUtil 菜单树 + 策略模式支付/折扣")
                        .version("1.0.0"));
    }
}

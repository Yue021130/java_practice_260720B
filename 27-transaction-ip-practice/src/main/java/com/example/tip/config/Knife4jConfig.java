package com.example.tip.config;

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
                        .title("第27章：事务失效与 IP 归属地实战")
                        .description("@Transactional 失效场景 + ip2region IP 解析")
                        .version("1.0.0"));
    }
}

package com.example.caa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第 20 章启动类：自定义注解 + AOP 高阶玩法实战。
 *
 * <p>Spring Boot 2.x 引入 spring-boot-starter-aop 后会自动开启 @EnableAspectJAutoProxy，
 * 无需手动标注。</p>
 */
@SpringBootApplication
public class CustomAnnotationAopPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomAnnotationAopPracticeApplication.class, args);
    }
}

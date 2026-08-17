package com.example.os;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 第 18 章启动类：Optional + Stream 真实业务场景实践。
 */
@SpringBootApplication
@EnableConfigurationProperties
public class OptionalStreamPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(OptionalStreamPracticeApplication.class, args);
    }
}

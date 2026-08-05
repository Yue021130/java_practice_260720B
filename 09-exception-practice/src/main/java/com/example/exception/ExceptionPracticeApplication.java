package com.example.exception;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Java 异常体系全场景实践启动类。
 *
 * 启用 @EnableAsync 用于演示异步任务中的异常处理。
 */
@SpringBootApplication
@EnableAsync
public class ExceptionPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExceptionPracticeApplication.class, args);
    }
}

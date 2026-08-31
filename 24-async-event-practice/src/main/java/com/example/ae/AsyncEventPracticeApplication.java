package com.example.ae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务与 Spring Event 实战启动类。
 *
 * <p>@EnableAsync 开启 Spring 异步代理支持，@Async 才会生效。</p>
 */
@EnableAsync
@SpringBootApplication
public class AsyncEventPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncEventPracticeApplication.class, args);
    }
}

package com.example.threadpool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 线程池实践学习项目启动类。
 *
 * 本专题通过三个不同定位的线程池（cpuPool / ioPool / customPool），
 * 配合前端实时监控面板，直观演示线程池七大参数与拒绝策略的效果。
 */
@SpringBootApplication
public class ThreadPoolPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreadPoolPracticeApplication.class, args);
    }
}

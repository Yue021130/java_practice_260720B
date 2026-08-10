package com.example.comm;

import com.example.comm.config.CommPracticeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Java 线程间通信方式实践启动类。
 *
 * 把「线程间通信七大类」转化为可运行、可交互的 Spring Boot + Vue 3 项目：
 * - 共享内存（volatile / 原子类）
 * - 等待通知（wait-notify / Condition）
 * - 线程协作控制（join / interrupt / LockSupport）
 * - JUC 同步工具（CountDownLatch / CyclicBarrier / Semaphore / Exchanger / Phaser）
 * - 阻塞队列（BlockingQueue 家族）
 * - 异步结果传递（Future / CompletableFuture）
 * - 管道与其他通道（PipedStream / 跨进程思路）
 *
 * 全部场景开箱即用：不依赖任何外部服务，多线程演示都在内存里编排，
 * 直接 {@code mvn spring-boot:run} 就能玩。
 */
@SpringBootApplication
@EnableConfigurationProperties(CommPracticeProperties.class)
public class ThreadCommunicationPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreadCommunicationPracticeApplication.class, args);
    }
}

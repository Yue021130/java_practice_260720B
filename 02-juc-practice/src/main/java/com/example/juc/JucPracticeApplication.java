package com.example.juc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 并发包（JUC）全场景实践学习项目启动类。
 *
 * 本专题把 java.util.concurrent 的核心知识点（锁、原子类、并发容器、
 * 同步工具、Future/CompletableFuture、ThreadLocal、JMM 基础）包装成
 * 可运行的现实业务场景，配合前端面板观察多线程行为。
 */
@SpringBootApplication
public class JucPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JucPracticeApplication.class, args);
    }
}

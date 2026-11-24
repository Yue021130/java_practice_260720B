package com.example.tlp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 第37章启动类：日志优雅可追溯实战。
 *
 * <p>场景：traceId 在同步/异步/Future/定时任务/事件/异常链路的
 * 全链路串联；logback 分时分天切割 + 按级别分文件 + 手机号脱敏；
 * Actuator 动态日志级别；批次任务线程池进度可视化。</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TlpApplication {

    public static void main(String[] args) {
        SpringApplication.run(TlpApplication.class, args);
        System.out.println("\n>>> 第37章启动成功，日志输出到 logs/ 目录 <<<\n");
    }
}

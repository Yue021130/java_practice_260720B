package com.example.sep;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 第34章启动类：Vue3 + SpringBoot 前后端分离实战。
 *
 * <p>业务背景：请假审批系统，演示 Spring Event 事件监听综合实战——
 * 普通 POJO 事件、@EventListener 同步监听、@TransactionalEventListener
 * （AFTER_COMMIT 事务事件）、@Async 异步监听、@Order 顺序、condition 条件
 * SpEL、event_record 本地消息表可靠性落地。</p>
 *
 * <p>@EnableAsync：开启 @Async 异步支持。不开启的话 @Async 注解
 * 不生效，方法会在调用线程同步执行（本章最常见的「异步没异步」坑）。</p>
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.example.sep.mapper")
public class SepApplication {

    public static void main(String[] args) {
        SpringApplication.run(SepApplication.class, args);
        System.out.println("\n>>> 第34章启动成功，Knife4j 文档: http://localhost:8080/doc.html <<<\n");
    }
}

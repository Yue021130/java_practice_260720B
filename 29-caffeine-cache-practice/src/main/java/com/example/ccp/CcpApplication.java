package com.example.ccp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第29章启动类：Vue3 + SpringBoot 前后端分离实战。
 *
 * <p>业务背景：后台用户管理系统，演示 Sa-Token JWT 无状态登录认证 + Caffeine 本地缓存。</p>
 */
@SpringBootApplication
@MapperScan("com.example.ccp.mapper")
public class CcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcpApplication.class, args);
        System.out.println("\n>>> 第29章启动成功，Knife4j 文档: http://localhost:8080/doc.html <<<\n");
    }
}

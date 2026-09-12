package com.example.sfp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第30章启动类：Vue3 + SpringBoot 前后端分离实战。
 *
 * <p>业务背景：客户信息管理系统，演示 Jakarta Validation 参数校验 + Jackson 响应数据脱敏。</p>
 */
@SpringBootApplication
@MapperScan("com.example.sfp.mapper")
public class SfpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SfpApplication.class, args);
        System.out.println("\n>>> 第30章启动成功，Knife4j 文档: http://localhost:8080/doc.html <<<\n");
    }
}

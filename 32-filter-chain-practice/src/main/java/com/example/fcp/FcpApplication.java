package com.example.fcp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 第32章启动类：Vue3 + SpringBoot 前后端分离实战。
 *
 * <p>业务背景：公告管理系统，演示 Servlet Filter 过滤器链
 *（TraceId 全链路 / 编码 / XSS 清洗 / 耗时审计 / Filter 层登录校验）。</p>
 *
 * <p>@ServletComponentScan：扫描 @WebFilter / @WebServlet / @WebListener，
 * 使 XssFilter 的注解注册方式生效。</p>
 */
@SpringBootApplication
@ServletComponentScan
@MapperScan("com.example.fcp.mapper")
public class FcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcpApplication.class, args);
        System.out.println("\n>>> 第32章启动成功，Knife4j 文档: http://localhost:8080/doc.html <<<\n");
    }
}

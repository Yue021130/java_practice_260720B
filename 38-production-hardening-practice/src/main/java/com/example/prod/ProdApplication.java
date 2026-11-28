package com.example.prod;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第38章启动类：生产级 Spring Boot 加固实战。
 *
 * <p>覆盖 spec 八节：密钥外置与 DevTools 禁用、Spring Security + JWT
 * 认证授权（含黑名单撤销）、HTTPS/限流/CORS、JSON 结构化日志与
 * traceId、Actuator 健康检查与 Prometheus、springdoc Profile 治理、
 * 测试金字塔（Testcontainers）、OWASP 依赖治理与混沌工程、审计合规
 * 与灾备策略。</p>
 */
@SpringBootApplication
@MapperScan("com.example.prod.mapper")
public class ProdApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProdApplication.class, args);
        System.out.println("\n>>> 第38章启动成功（生产加固版） <<<\n");
    }
}

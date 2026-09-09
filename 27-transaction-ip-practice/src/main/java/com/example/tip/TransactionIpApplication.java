package com.example.tip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 第27章启动类：事务失效 + IP 归属地实战
 *
 * <p>业务背景：
 * 1. 事务失效是线上常见问题，本章用 H2 数据库演示 5 种典型失效场景；
 * 2. ip2region 是常用的离线 IP 库，可快速把访问 IP 解析为国家/省/市/运营商。
 * </p>
 */
@EnableAsync
@SpringBootApplication
@MapperScan("com.example.tip.mapper")
public class TransactionIpApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionIpApplication.class, args);
    }
}

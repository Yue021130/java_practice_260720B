package com.example.ts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第26章启动类：树形结构 + 策略模式实战
 *
 * <p>业务背景：
 * 1. 后台管理系统左侧菜单通常是一张“扁平”的权限表（id / parent_id），需要转成树形返回给前端；
 * 2. 电商/支付系统中支付方式、促销折扣经常变化，使用策略模式把变化点封装成可插拔的算法族。
 * </p>
 */
@SpringBootApplication
public class TreeStrategyApplication {
    public static void main(String[] args) {
        SpringApplication.run(TreeStrategyApplication.class, args);
    }
}

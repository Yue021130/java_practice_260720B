package com.example.scp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 第33章启动类：Vue3 + SpringBoot 前后端分离实战。
 *
 * <p>业务背景：商品管理系统，演示 Spring Cache 缓存框架综合实战——
 * 自定义 Caffeine（一级）+ Redis（二级）二级缓存组件，覆盖
 * @Cacheable / @CacheEvict / @CachePut / @Caching 注解、自定义 KeyGenerator、
 * 缓存穿透 / 击穿 / 雪崩应对与命中率监控。</p>
 *
 * <p>@EnableCaching：开启 Spring Cache 注解驱动（@Cacheable 等注解由
 * Spring AOP 代理生效，不开启则注解不工作）。</p>
 */
@SpringBootApplication
@EnableCaching
@MapperScan("com.example.scp.mapper")
public class ScpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScpApplication.class, args);
        System.out.println("\n>>> 第33章启动成功，Knife4j 文档: http://localhost:8080/doc.html <<<\n");
    }
}

package com.example.sign;

import com.example.sign.config.SignPracticeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 基于 appid + appkey 的 HMAC-SHA256 接口签名鉴权实践启动类。
 *
 * 把业界最成熟的接口鉴权方案（HMAC-SHA256 请求签名，AWS / 阿里云 / 微信支付同款）
 * 转化为可运行、可交互的 Spring Boot + Vue 3 项目：
 * - 核心原理：appid / appkey / 签名三要素与鉴权流程
 * - 签名计算：Canonical String 9 字段 + HMAC-SHA256
 * - 服务端验签：时间戳 / nonce 防重放 + 签名比对
 * - 请求体完整性：Content-MD5 / HashedPayload
 * - 规范化、简化版、@RequireSign 拦截器实战、选型对比
 *
 * 全部场景开箱即用：不依赖任何外部服务（Redis 用内存模拟），
 * 直接 {@code mvn spring-boot:run} 就能玩。
 */
@SpringBootApplication
@EnableConfigurationProperties(SignPracticeProperties.class)
public class ApiSignaturePracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiSignaturePracticeApplication.class, args);
    }
}

package com.example.sign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接口签名鉴权实践自定义配置（前缀 sign.practice，见 application.yml）。
 *
 * 用 @EnableConfigurationProperties 注册为 Bean，各 Service 注入读取，
 * 让「时间戳窗口 / nonce 有效期 / 演示 appid-appkey」等参数可调，
 * 也方便测试里覆盖调小。
 */
@Data
@ConfigurationProperties(prefix = "sign.practice")
public class SignPracticeProperties {

    /** 防重放：时间戳允许的最大偏差（秒），默认 ±5 分钟 */
    private long timestampSkewSeconds = 300;

    /** nonce 有效期（秒），与时间戳窗口一致 */
    private long nonceTtlSeconds = 300;

    /** 演示用注册的 appid */
    private String demoAppId = "demo_app_001";

    /** 演示用 appkey（生产绝不落库明文，这里教学演示） */
    private String demoAppKey = "d1c4a5b9f2e7e0c8d3b6a1f4e9c2b8d7a5f3e1c0b9d8a7c6b5f4e3d2c1a0b9f8";
}

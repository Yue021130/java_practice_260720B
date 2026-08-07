package com.example.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件实践配置（前缀 mail.practice）。
 *
 * 自定义配置前缀，避免与 Spring Boot 的 spring.mail.* 自动配置冲突。
 * 默认 simulate 模式：不真正连接 SMTP，只构造并记录邮件内容，开箱即用。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail.practice")
public class MailPracticeProperties {

    /** 发送模式：simulate=模拟发送（默认），real=真实发送 */
    private String mode = "simulate";

    /** 默认发件人 */
    private String from = "zsx-mail-practice@example.com";

    /** SMTP 服务器地址 */
    private String host = "smtp.qq.com";

    /** SMTP 端口 */
    private int port = 465;

    /** 协议：smtp / smtps */
    private String protocol = "smtp";

    /** 登录账号（通常就是发件邮箱） */
    private String username = "";

    /** 授权码（不是邮箱密码，是 SMTP 服务商签发的授权码） */
    private String password = "";

    /** 是否启用 SSL（QQ 邮箱 465 需要） */
    private boolean ssl = false;

    /** 是否启用 STARTTLS（163 邮箱 25 端口需要） */
    private boolean starttls = false;

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 10000;

    /** 读取超时（毫秒） */
    private int readTimeoutMs = 10000;

    /** 写入超时（毫秒） */
    private int writeTimeoutMs = 10000;

    /** 失败最大重试次数 */
    private int maxRetries = 3;

    /** 重试基础退避毫秒（指数退避的基数） */
    private int retryBaseDelayMs = 500;

    /** 定时演示任务开关：true 时每分钟发一封心跳邮件（real 模式请谨慎开启） */
    private boolean scheduleDemo = false;

    /** Quartz 演示任务开关：true 时启动即注册一个每 30 秒触发一次的 Quartz 任务（simulate 模式便于观察） */
    private boolean quartzDemo = false;
}

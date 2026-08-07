package com.example.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 邮件服务实践启动类。
 *
 * - @EnableAsync：开启异步发送邮件能力
 * - @EnableScheduling：开启定时/批量发送演示能力
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class MailPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailPracticeApplication.class, args);
    }
}

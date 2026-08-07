package com.example.mail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 邮件基础设施配置。
 *
 * 1. 手动构建 JavaMailSenderImpl（而非依赖 spring.mail.* 自动配置）：
 *    Spring Boot 只有在配置了 spring.mail.host 时才会自动装配 JavaMailSender，
 *    本专题默认 simulate 模式不配置 host，因此手动创建，保证应用总能启动。
 * 2. 提供异步发送专用线程池 mailExecutor。
 */
@Configuration
public class MailConfig {

    /**
     * JavaMailSender：负责创建 MimeMessage 与真实发送。
     *
     * 注意：createMimeMessage() 只是内存中构造消息，不会连接 SMTP；
     * 是否真正连接服务器由 {@link com.example.mail.service.MailDeliveryService} 按 mode 决定。
     */
    @Bean
    public JavaMailSenderImpl javaMailSender(MailPracticeProperties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getHost());
        sender.setPort(props.getPort());
        // 注意：setUsername 传空字符串会被 Spring 当作“需要认证”，从而用空凭据去 AUTH 导致 535。
        // 只有真正配置了账号才设置 username/password；未配置时走免认证（如本地 GreenMail）。
        if (props.getUsername() != null && !props.getUsername().isEmpty()) {
            sender.setUsername(props.getUsername());
            sender.setPassword(props.getPassword());
        }
        sender.setProtocol(props.getProtocol());
        sender.setDefaultEncoding("UTF-8");

        Properties javaMailProps = new Properties();
        javaMailProps.put("mail.smtp.auth",
                props.getUsername() != null && !props.getUsername().isEmpty() ? "true" : "false");
        javaMailProps.put("mail.smtp.connectiontimeout", props.getConnectTimeoutMs());
        javaMailProps.put("mail.smtp.timeout", props.getReadTimeoutMs());
        javaMailProps.put("mail.smtp.writetimeout", props.getWriteTimeoutMs());
        if (props.isSsl()) {
            javaMailProps.put("mail.smtp.ssl.enable", "true");
        }
        if (props.isStarttls()) {
            javaMailProps.put("mail.smtp.starttls.enable", "true");
        }
        sender.setJavaMailProperties(javaMailProps);
        return sender;
    }

    /**
     * 异步发送邮件专用线程池。
     *
     * - 核心线程数适中：邮件发送是 IO 密集 + 少量 CPU，占用不高
     * - 队列有界：防止任务无限堆积
     * - CallerRuns 拒绝策略：线程池饱和时由调用线程自己发送，保证不丢邮件
     */
    @Bean("mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mail-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * 定时/延迟发送专用调度线程池（单线程，守护线程）。
     */
    @Bean("mailScheduler")
    public ScheduledExecutorService mailScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mail-schedule-");
            t.setDaemon(true);
            return t;
        });
    }
}

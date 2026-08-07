package com.example.mail.schedule;

import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;

/**
 * Quartz 任务执行委托。
 *
 * 为什么需要它：Quartz 用自己的 JobFactory 实例化 Job（每次触发 new 一个新实例），
 * 不经过 Spring 容器，因此 Job 里无法直接 @Autowired。常规做法有：
 * 1. 自定义 SpringBeanJobFactory 让 Quartz 实例走 Spring 装配（生产推荐）；
 * 2. 用一个静态持有的 Spring Bean 作为委托（本项目采用，简单直观）。
 *
 * 本类作为 @Component 被 Spring 装配，构造时把自身存入静态字段，
 * MailCronJob 通过 QuartzJobDelegate.get() 取到 Spring 托管的 Bean 来真正发邮件。
 */
@Slf4j
@Component
public class QuartzJobDelegate {

    private static QuartzJobDelegate instance;

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;

    public QuartzJobDelegate(JavaMailSenderImpl mailSender,
                             MailPracticeProperties props,
                             MailDeliveryService deliveryService) {
        this.mailSender = mailSender;
        this.props = props;
        this.deliveryService = deliveryService;
        QuartzJobDelegate.instance = this;
    }

    public static QuartzJobDelegate get() {
        return instance;
    }

    /**
     * 由 Quartz Job 触发的发送入口。
     */
    public void sendFromJob(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);
            helper.setText(content);
            // 发送完成后同样会发布 MailSentEvent，事件监听器也会响应
            deliveryService.send(message, "quartz", "Quartz 定时任务触发的邮件");
            log.info("[Quartz] 任务已执行：subject={} to={}", subject, to);
        } catch (Exception e) {
            log.error("[Quartz] 任务执行失败：subject={}", subject, e);
        }
    }
}

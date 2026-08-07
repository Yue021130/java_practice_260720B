package com.example.mail.service;

import com.example.mail.config.MailPracticeProperties;
import com.example.mail.event.MailSentEvent;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailRecordStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;

/**
 * 邮件投递服务（核心）。
 *
 * 所有发送入口都汇聚到这里：
 * - simulate 模式：只构造 MimeMessage、打印内容并记录，不连接 SMTP（开箱即用）
 * - real 模式：真正通过 JavaMailSender 发送到 SMTP 服务器
 *
 * 返回的 {@link MailRecord} 会写入记录存储，供「最近发送记录」面板展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailDeliveryService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailRecordStore recordStore;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 当前是否为模拟发送模式。
     */
    public boolean isSimulate() {
        return "simulate".equalsIgnoreCase(props.getMode());
    }

    /**
     * 发送（或模拟发送）一封已构造好的 MimeMessage。
     *
     * @param message 已填充内容的 MimeMessage
     * @param tag     场景标签（basic / html / ...）
     * @param note    备注，用于面板区分场景
     */
    public MailRecord send(MimeMessage message, String tag, String note) throws MessagingException {
        long start = System.currentTimeMillis();

        try {
            // saveChanges 会把消息内容写入内存字节流，之后 getSize() 才有值，
            // 同时把中文主题按 RFC 2047 编码到 header
            message.saveChanges();

            if (isSimulate()) {
                log.info("[模拟发送][{}] 主题={} 收件人={} 大小={}B",
                        tag, message.getSubject(), MailRecord.format(message.getAllRecipients()), message.getSize());
            } else {
                mailSender.send(message);
            }

            long costMs = System.currentTimeMillis() - start;
            MailRecord record = MailRecord.of(tag, message, isSimulate(), note, costMs);
            recordStore.add(record);
            log.info("[发送完成][{}] 耗时={}ms 模式={}", tag, costMs, isSimulate() ? "simulate" : "real");

            // 发布成功事件，触发 @EventListener 链路（日志/统计/通知）
            eventPublisher.publishEvent(new MailSentEvent(record, true, null));
            return record;
        } catch (MessagingException e) {
            // 真实发送失败也发布失败事件，供统计/告警监听器使用
            eventPublisher.publishEvent(new MailSentEvent(null, false, e.getMessage()));
            throw e;
        }
    }

    /**
     * 便捷重载：无备注。
     */
    public MailRecord send(MimeMessage message, String tag) throws MessagingException {
        return send(message, tag, null);
    }

    /**
     * 最近发送记录。
     */
    public java.util.List<MailRecord> recentRecords() {
        return recordStore.recent();
    }
}

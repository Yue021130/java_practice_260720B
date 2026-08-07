package com.example.mail.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听器一：日志监听器。
 *
 * 最简单的 @EventListener：同步监听，在发送线程内执行。
 * 事件发布后本方法立即被调用，用于记录审计日志、打点等轻量操作。
 */
@Slf4j
@Component
public class MailLogEventListener {

    /**
     * 通过方法参数类型自动匹配事件类型 MailSentEvent。
     */
    @EventListener
    public void onMailSent(MailSentEvent event) {
        if (event.isSuccess()) {
            // 手动发布演示事件时 record 可能为 null，需判空
            if (event.getRecord() == null) {
                log.info("[事件监听] 收到 MailSentEvent：发送成功（无记录详情，来自演示发布）");
                return;
            }
            log.info("[事件监听] 收到 MailSentEvent：邮件发送成功 tag={} subject={} 收件人={}",
                    event.getRecord().getTag(), event.getRecord().getSubject(), event.getRecord().getTo());
        } else {
            log.warn("[事件监听] 收到 MailSentEvent：邮件发送失败 error={}", event.getError());
        }
    }
}

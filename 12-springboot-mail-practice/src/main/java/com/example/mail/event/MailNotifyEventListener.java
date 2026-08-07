package com.example.mail.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 监听器三：异步通知监听器。
 *
 * 演示 @Async + @EventListener 组合：监听器里做重/慢的副作用（如发短信、站内推送、
 * 对接第三方 webhook）时，异步执行可避免拖慢发送线程。
 * 这里 sleep 500ms 模拟一次外部 HTTP 调用，注意发送线程本身没有等待。
 */
@Slf4j
@Component
public class MailNotifyEventListener {

    /**
     * 只处理成功事件；异步在 mailExecutor 线程池中执行。
     */
    @Async("mailExecutor")
    @EventListener(condition = "#event.success")
    public void notifyUser(MailSentEvent event) {
        long start = System.currentTimeMillis();
        try {
            // 模拟调用站内信 / 短信 / 第三方通知服务
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[事件监听-异步] 已向用户发送站内通知（耗时 {}ms），邮件主题={}",
                System.currentTimeMillis() - start,
                event.getRecord() != null ? event.getRecord().getSubject() : "-");
    }
}

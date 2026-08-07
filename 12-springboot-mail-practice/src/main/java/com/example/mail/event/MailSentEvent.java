package com.example.mail.event;

import com.example.mail.support.MailRecord;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 邮件发送结果事件。
 *
 * Spring 4.2 之后 @EventListener 可以监听任意 POJO，不必继承 ApplicationEvent。
 * 由 MailDeliveryService 在发送完成后发布；成功与失败都会发布，便于下游统一处理。
 */
@Data
@AllArgsConstructor
public class MailSentEvent {

    /** 发送成功的邮件记录；失败时为 null */
    private final MailRecord record;

    /** 是否发送成功 */
    private final boolean success;

    /** 失败原因；成功时为 null */
    private final String error;
}

package com.example.tlp.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 模拟 MQ 消息：用 Spring 事件代替真实 Broker，
 * 验证「发布 → 异步消费」链路上的 traceId 透传。
 */
@Getter
public class DemoEvent extends ApplicationEvent {

    private final String orderId;

    public DemoEvent(String orderId) {
        super(orderId);
        this.orderId = orderId;
    }
}

package com.example.tlp.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 模拟 MQ 生产者：发布事件（真实环境换成发消息到 Broker，
 * 并在消息头携带 X-Trace-Id，消费端取出来放回 MDC）。
 */
@Component
public class DemoEventPublisher {

    private final ApplicationEventPublisher publisher;

    public DemoEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(String orderId) {
        publisher.publishEvent(new DemoEvent(orderId));
    }
}

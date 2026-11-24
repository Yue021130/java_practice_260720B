package com.example.tlp.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 模拟 MQ 消费者：@Async 异步处理。
 *
 * <p>能拿到发布时刻的 traceId，靠两条：@Async 走 batchExecutor
 * （配了 MdcTaskDecorator）；真实 MQ 则靠消息头透传，消费端
 * 手动 set 回 MDC。</p>
 */
@Slf4j
@Component
public class DemoEventListener {

    @Async("batchExecutor")
    @EventListener
    public void onEvent(DemoEvent event) {
        log.info("模拟 MQ 消费：处理订单 {}", event.getOrderId());
    }
}

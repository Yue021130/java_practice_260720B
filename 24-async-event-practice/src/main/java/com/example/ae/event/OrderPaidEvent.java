package com.example.ae.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 订单已支付事件。
 *
 * <p>继承 ApplicationEvent，携带订单号与金额，供多个监听器异步处理。</p>
 */
@Getter
public class OrderPaidEvent extends ApplicationEvent {

    private final String orderNo;
    private final Long userId;
    private final java.math.BigDecimal amount;

    public OrderPaidEvent(Object source, String orderNo, Long userId, java.math.BigDecimal amount) {
        super(source);
        this.orderNo = orderNo;
        this.userId = userId;
        this.amount = amount;
    }
}

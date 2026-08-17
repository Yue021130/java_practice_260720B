package com.example.os.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体：模拟电商订单，用于报表统计场景。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;
    private Long userId;
    private BigDecimal amount;
    private OrderStatus status;
    private LocalDateTime createTime;

    /**
     * 订单状态：用于 Stream.filter 分组统计。
     */
    public enum OrderStatus {
        PAID,
        SHIPPED,
        COMPLETED,
        CANCELLED
    }
}

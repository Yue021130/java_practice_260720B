package com.example.ae.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，模拟电商下单场景。
 */
@Data
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号。 */
    @Column(length = 64, unique = true)
    private String orderNo;

    /** 用户 ID。 */
    private Long userId;

    /** 订单金额。 */
    private BigDecimal amount;

    /** 订单状态：0-待支付，1-已支付，2-已取消。 */
    private Integer status;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 支付时间。 */
    private LocalDateTime payTime;
}

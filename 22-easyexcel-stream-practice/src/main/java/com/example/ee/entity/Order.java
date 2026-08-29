package com.example.ee.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，模拟待导出的大表数据。
 */
@Data
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单编号。 */
    @Column(length = 64)
    private String orderNo;

    /** 用户 ID。 */
    private Long userId;

    /** 用户名。 */
    @Column(length = 64)
    private String username;

    /** 商品名称。 */
    @Column(length = 128)
    private String productName;

    /** 订单金额。 */
    private BigDecimal amount;

    /** 订单状态。 */
    @Column(length = 32)
    private String status;

    /** 下单时间。 */
    private LocalDateTime orderTime;

    /** 收货地址。 */
    @Column(length = 256)
    private String address;

    /** 备注。 */
    @Column(length = 512)
    private String remark;
}

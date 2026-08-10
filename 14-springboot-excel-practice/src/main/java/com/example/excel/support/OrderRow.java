package com.example.excel.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单演示数据（各模块共享的数据源）。
 *
 * 用于复杂表头（年度销售汇总）与模板填充（订单明细）等场景。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRow {

    /** 订单号 */
    private String orderNo;

    /** 客户名 */
    private String customer;

    /** 订单金额 */
    private BigDecimal amount;

    /** 状态：已支付 / 待支付 / 已取消 */
    private String status;

    /** 下单时间 */
    private Date createTime;
}

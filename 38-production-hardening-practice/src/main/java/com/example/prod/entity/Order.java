package com.example.prod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务订单：方法级权限（@PreAuthorize）与审计日志的演示载体
 */
@Data
@TableName("biz_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（业务唯一，演示简化不做唯一约束） */
    private String orderNo;

    /** 金额 */
    private BigDecimal amount;

    /** 所属部门（演示数据权限维度，当前固定 1） */
    private Long deptId;

    /** 下单人 sys_user.id */
    private Long userId;

    /** 状态：1 正常 / 0 取消 */
    private Integer status;

    private LocalDateTime createTime;
}

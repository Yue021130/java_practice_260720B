package com.example.epc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应表 biz_order。
 *
 * <p>本章数据权限的演练对象：deptId / userId 两个归属字段是
 * DataScopeHandler（SQL 注入过滤）和 DataScopeValidator（详情越权校验）
 * 的判断依据。</p>
 */
@Data
@TableName("biz_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（唯一） */
    private String orderNo;

    /** 订单金额 */
    private BigDecimal amount;

    /** 归属部门 id（数据权限过滤维度） */
    private Long deptId;

    /** 归属用户 id（SELF 数据权限过滤维度） */
    private Long userId;

    /** 状态：1 正常，0 已取消 */
    private Integer status;

    private LocalDateTime createTime;
}

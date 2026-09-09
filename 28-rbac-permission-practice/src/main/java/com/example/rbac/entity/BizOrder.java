package com.example.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_order")
public class BizOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 下单人 */
    private Long userId;

    /** 所属部门（数据权限过滤维度） */
    private Long deptId;

    /** 金额 */
    private BigDecimal amount;

    /** 状态：PAID 已支付 / REFUNDED 已退款 */
    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}

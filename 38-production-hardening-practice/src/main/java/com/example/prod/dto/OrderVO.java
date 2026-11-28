package com.example.prod.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象：订单全部字段 + 下单人用户名
 */
@Data
@Schema(description = "订单视图对象")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单 ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "下单人用户 ID")
    private Long userId;

    @Schema(description = "下单人用户名（由 userId 批量关联得出）")
    private String username;

    @Schema(description = "状态：1 正常 / 0 取消")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

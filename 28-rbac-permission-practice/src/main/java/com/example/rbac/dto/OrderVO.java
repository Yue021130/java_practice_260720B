package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单分页记录（已按数据权限过滤）")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单 ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "下单人 ID")
    private Long userId;

    @Schema(description = "下单人昵称")
    private String nickname;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "状态：PAID 已支付 / REFUNDED 已退款")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

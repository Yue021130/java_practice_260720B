package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "下单请求")
public class OrderSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    @Schema(description = "订单金额")
    private BigDecimal amount;

    @Schema(description = "下单部门 ID（不填默认为当前用户部门）")
    private Long deptId;
}

package com.example.prod.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单保存请求（新增/修改共用；修改按 orderNo 定位订单）
 */
@Data
@Schema(description = "订单保存请求")
public class OrderSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号（业务键，修改时按它定位订单）")
    private String orderNo;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    @Schema(description = "订单金额")
    private BigDecimal amount;

    @Schema(description = "状态：1 正常 / 0 取消，默认 1")
    private Integer status;
}

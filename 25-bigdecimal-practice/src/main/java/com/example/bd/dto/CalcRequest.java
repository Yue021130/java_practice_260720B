package com.example.bd.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 金额计算请求。
 *
 * <p>税率 taxRate 为百分比数值，如 6 表示 6%。</p>
 */
@Data
public class CalcRequest {

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "单价必须大于 0")
    private BigDecimal price;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    @DecimalMin(value = "0.00", inclusive = true, message = "折扣不能为负数")
    private BigDecimal discount;

    @DecimalMin(value = "0.00", inclusive = true, message = "税率不能为负数")
    private BigDecimal taxRate;

    @NotNull(message = "精度不能为空")
    @Min(value = 0, message = "精度不能小于 0")
    @Max(value = 10, message = "精度不能大于 10")
    private Integer scale;
}

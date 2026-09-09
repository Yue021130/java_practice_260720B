package com.example.ts.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 折扣计算请求 DTO
 */
@Data
public class DiscountRequestDTO {

    /** 折扣策略：NORMAL / VIP / SEASONAL */
    @NotBlank(message = "折扣策略不能为空")
    private String strategyType;

    /** 原始金额 */
    @NotNull(message = "原始金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "原始金额不能小于 0")
    private BigDecimal originalAmount;
}

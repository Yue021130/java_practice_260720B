package com.example.ts.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 支付请求 DTO
 */
@Data
public class PayRequestDTO {

    /** 支付方式：ALIPAY / WECHAT / BALANCE */
    @NotBlank(message = "支付方式不能为空")
    private String payType;

    /** 订单号 */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** 支付金额 */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于 0")
    private BigDecimal amount;
}

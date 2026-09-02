package com.example.bd.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 金额计算请求。
 */
@Data
public class CalcRequest {

    private BigDecimal price;
    private Integer quantity;
    private BigDecimal discount;
    private BigDecimal taxRate;
    private Integer scale;
}

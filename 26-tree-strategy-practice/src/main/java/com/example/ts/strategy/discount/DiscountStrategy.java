package com.example.ts.strategy.discount;

import com.example.ts.dto.DiscountRequestDTO;

import java.math.BigDecimal;

/**
 * 折扣策略接口
 */
public interface DiscountStrategy {

    /**
     * 策略类型标识
     */
    String getType();

    /**
     * 计算折扣后金额
     *
     * @param request 折扣请求
     * @return 折扣后金额
     */
    BigDecimal calculate(DiscountRequestDTO request);
}

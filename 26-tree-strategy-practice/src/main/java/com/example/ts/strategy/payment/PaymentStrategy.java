package com.example.ts.strategy.payment;

import com.example.ts.dto.PayRequestDTO;

import java.math.BigDecimal;

/**
 * 支付策略接口
 *
 * <p>策略模式八股：
 * 1. 定义算法族（多种支付方式），分别封装起来，让它们可以互相替换；
 * 2. 将算法的变化独立于使用算法的客户；
 * 3. 配合 Spring 的 Map&lt;String, T&gt; 注入，可实现运行时动态选择策略。
 * </p>
 */
public interface PaymentStrategy {

    /**
     * 策略类型标识
     */
    String getType();

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 实际支付金额（可能涉及优惠、手续费等）
     */
    BigDecimal pay(PayRequestDTO request);
}

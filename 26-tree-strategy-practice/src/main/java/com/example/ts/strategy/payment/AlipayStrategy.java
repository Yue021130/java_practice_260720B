package com.example.ts.strategy.payment;

import com.example.ts.common.BusinessException;
import com.example.ts.dto.PayRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 支付宝支付策略
 *
 * <p>假设规则：支付宝收取 0.6% 手续费，最低 0.01 元。</p>
 */
@Slf4j
@Component
public class AlipayStrategy implements PaymentStrategy {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.006");

    @Override
    public String getType() {
        return "ALIPAY";
    }

    @Override
    public BigDecimal pay(PayRequestDTO request) {
        if (request.getAmount().compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException("支付宝支付金额不能小于 0.01 元");
        }
        BigDecimal fee = request.getAmount().multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        if (fee.compareTo(new BigDecimal("0.01")) < 0) {
            fee = new BigDecimal("0.01");
        }
        BigDecimal actual = request.getAmount().add(fee);
        log.info("【支付宝】订单 {} 支付金额 {} 手续费 {} 实付 {}",
                request.getOrderNo(), request.getAmount(), fee, actual);
        return actual;
    }
}

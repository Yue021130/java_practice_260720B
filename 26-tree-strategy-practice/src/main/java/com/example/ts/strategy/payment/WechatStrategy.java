package com.example.ts.strategy.payment;

import com.example.ts.dto.PayRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 微信支付策略
 *
 * <p>假设规则：微信支付手续费 0.38%，无最低限制。</p>
 */
@Slf4j
@Component
public class WechatStrategy implements PaymentStrategy {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.0038");

    @Override
    public String getType() {
        return "WECHAT";
    }

    @Override
    public BigDecimal pay(PayRequestDTO request) {
        BigDecimal fee = request.getAmount().multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actual = request.getAmount().add(fee);
        log.info("【微信】订单 {} 支付金额 {} 手续费 {} 实付 {}",
                request.getOrderNo(), request.getAmount(), fee, actual);
        return actual;
    }
}

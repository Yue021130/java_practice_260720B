package com.example.ts.strategy.payment;

import com.example.ts.common.BusinessException;
import com.example.ts.dto.PayRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 余额支付策略
 *
 * <p>假设规则：余额支付无手续费，但余额不足时抛业务异常。
 * 这里用伪余额 1000.00 元做演示。</p>
 */
@Slf4j
@Component
public class BalanceStrategy implements PaymentStrategy {

    // 演示：模拟账户余额
    private static final BigDecimal MOCK_BALANCE = new BigDecimal("1000.00");

    @Override
    public String getType() {
        return "BALANCE";
    }

    @Override
    public BigDecimal pay(PayRequestDTO request) {
        if (request.getAmount().compareTo(MOCK_BALANCE) > 0) {
            throw new BusinessException("余额不足，当前余额：" + MOCK_BALANCE);
        }
        log.info("【余额】订单 {} 支付金额 {} 剩余余额 {}",
                request.getOrderNo(), request.getAmount(), MOCK_BALANCE.subtract(request.getAmount()));
        return request.getAmount();
    }
}

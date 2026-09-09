package com.example.ts.strategy.discount;

import com.example.ts.dto.DiscountRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 季节性折扣策略：满 200 减 50，可叠加 9 折
 *
 * <p>规则：先满减，再打折。</p>
 */
@Slf4j
@Component
public class SeasonalDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal THRESHOLD = new BigDecimal("200");
    private static final BigDecimal REDUCE = new BigDecimal("50");
    private static final BigDecimal EXTRA_RATE = new BigDecimal("0.9");

    @Override
    public String getType() {
        return "SEASONAL";
    }

    @Override
    public BigDecimal calculate(DiscountRequestDTO request) {
        BigDecimal amount = request.getOriginalAmount();
        if (amount.compareTo(THRESHOLD) >= 0) {
            amount = amount.subtract(REDUCE);
        }
        BigDecimal result = amount.multiply(EXTRA_RATE).setScale(2, RoundingMode.HALF_UP);
        log.info("【季节性促销】原价 {} 优惠后 {}", request.getOriginalAmount(), result);
        return result;
    }
}

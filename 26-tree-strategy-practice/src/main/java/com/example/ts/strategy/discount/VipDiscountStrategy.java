package com.example.ts.strategy.discount;

import com.example.ts.dto.DiscountRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * VIP 折扣策略：8.8 折
 */
@Slf4j
@Component
public class VipDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal VIP_RATE = new BigDecimal("0.88");

    @Override
    public String getType() {
        return "VIP";
    }

    @Override
    public BigDecimal calculate(DiscountRequestDTO request) {
        BigDecimal result = request.getOriginalAmount().multiply(VIP_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        log.info("【VIP】原价 {} 折后 {}", request.getOriginalAmount(), result);
        return result;
    }
}

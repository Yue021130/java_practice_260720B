package com.example.ts.strategy.discount;

import com.example.ts.dto.DiscountRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 普通会员折扣策略：无折扣
 */
@Slf4j
@Component
public class NormalDiscountStrategy implements DiscountStrategy {

    @Override
    public String getType() {
        return "NORMAL";
    }

    @Override
    public BigDecimal calculate(DiscountRequestDTO request) {
        log.info("【普通会员】原价 {} 无折扣", request.getOriginalAmount());
        return request.getOriginalAmount();
    }
}

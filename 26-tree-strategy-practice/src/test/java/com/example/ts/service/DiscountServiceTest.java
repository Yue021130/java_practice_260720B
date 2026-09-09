package com.example.ts.service;

import com.example.ts.dto.DiscountRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 折扣策略测试
 */
@SpringBootTest
class DiscountServiceTest {

    @Autowired
    private DiscountService discountService;

    @Test
    void testNormal() {
        DiscountRequestDTO dto = build("NORMAL", "100.00");
        assertEquals(new BigDecimal("100.00"), discountService.calculate(dto));
    }

    @Test
    void testVip() {
        DiscountRequestDTO dto = build("VIP", "100.00");
        // 100 * 0.88 = 88.00
        assertEquals(new BigDecimal("88.00"), discountService.calculate(dto));
    }

    @Test
    void testSeasonalLessThan200() {
        DiscountRequestDTO dto = build("SEASONAL", "100.00");
        // 不满 200 只打 9 折
        assertEquals(new BigDecimal("90.00"), discountService.calculate(dto));
    }

    @Test
    void testSeasonalGreaterThan200() {
        DiscountRequestDTO dto = build("SEASONAL", "300.00");
        // (300 - 50) * 0.9 = 225.00
        assertEquals(new BigDecimal("225.00"), discountService.calculate(dto));
    }

    private DiscountRequestDTO build(String type, String amount) {
        DiscountRequestDTO dto = new DiscountRequestDTO();
        dto.setStrategyType(type);
        dto.setOriginalAmount(new BigDecimal(amount));
        return dto;
    }
}

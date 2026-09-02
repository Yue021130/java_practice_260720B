package com.example.bd.service;

import com.example.bd.dto.CalcRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 金额计算服务测试。
 */
class AmountCalculatorTest {

    private final AmountCalculator calculator = new AmountCalculator();

    @Test
    void calculateOrderAmount_shouldReturnCorrectTotal() {
        CalcRequest req = new CalcRequest();
        req.setPrice(new BigDecimal("99.99"));
        req.setQuantity(3);
        req.setDiscount(new BigDecimal("0.9"));
        req.setTaxRate(new BigDecimal("6"));
        req.setScale(2);

        Map<String, Object> result = calculator.calculateOrderAmount(req);

        assertThat(result.get("originalAmount")).isEqualTo("299.97");
        assertThat(result.get("discountedAmount")).isEqualTo("269.97");
        assertThat(result.get("taxAmount")).isEqualTo("16.20");
        assertThat(result.get("totalAmount")).isEqualTo("286.17");
    }

    @Test
    void splitAmount_shouldKeepSumEqualsTotal() {
        Map<String, Object> result = calculator.splitAmount(
                new BigDecimal("100"),
                new BigDecimal("0.1"),
                new BigDecimal("0.2"),
                2);

        assertThat(result.get("total")).isEqualTo("100.00");
        assertThat(result.get("sumCheck")).isEqualTo(result.get("total"));
    }

    @Test
    void pitfalls_shouldShowDoubleProblem() {
        Map<String, Object> result = calculator.pitfalls();
        assertThat(result.get("new BigDecimal(0.1)").toString()).isNotEqualTo("0.1");
        assertThat(result.get("new BigDecimal(\"0.1\")")).isEqualTo("0.1");
        assertThat(result.get("BigDecimal.valueOf(0.1)")).isEqualTo("0.1");
    }
}

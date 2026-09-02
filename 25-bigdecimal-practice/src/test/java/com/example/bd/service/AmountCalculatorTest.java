package com.example.bd.service;

import com.example.bd.dto.CalcRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void calculateOrderAmount_withoutDiscountAndTax_shouldUseDefaults() {
        CalcRequest req = new CalcRequest();
        req.setPrice(new BigDecimal("100"));
        req.setQuantity(2);
        req.setScale(2);

        Map<String, Object> result = calculator.calculateOrderAmount(req);

        assertThat(result.get("discountedAmount")).isEqualTo("200.00");
        assertThat(result.get("taxAmount")).isEqualTo("0.00");
        assertThat(result.get("totalAmount")).isEqualTo("200.00");
    }

    @Test
    void calculateOrderAmount_withZeroScale_shouldReturnInteger() {
        CalcRequest req = new CalcRequest();
        req.setPrice(new BigDecimal("99.99"));
        req.setQuantity(3);
        req.setScale(0);

        Map<String, Object> result = calculator.calculateOrderAmount(req);

        assertThat(result.get("totalAmount")).isEqualTo("300");
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
        assertThat(result.get("remainingAmount")).isEqualTo("70.00");
    }

    @Test
    void splitAmount_withDivideRemainder_shouldKeepSumEqualsTotal() {
        Map<String, Object> result = calculator.splitAmount(
                new BigDecimal("100"),
                new BigDecimal("0.333"),
                new BigDecimal("0.333"),
                2);

        assertThat(result.get("sumCheck")).isEqualTo(result.get("total"));
        assertThat(result.get("remainingAmount")).isEqualTo("33.40");
    }

    @Test
    void splitAmount_shouldRejectNegativeRate() {
        assertThatThrownBy(() -> calculator.splitAmount(
                new BigDecimal("100"),
                new BigDecimal("-0.1"),
                new BigDecimal("0.2"),
                2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为负数");
    }

    @Test
    void splitAmount_shouldRejectSumGreaterThanOne() {
        assertThatThrownBy(() -> calculator.splitAmount(
                new BigDecimal("100"),
                new BigDecimal("0.6"),
                new BigDecimal("0.5"),
                2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能超过 1");
    }

    @Test
    void pitfalls_shouldShowDoubleProblem() {
        Map<String, Object> result = calculator.pitfalls();
        assertThat(result.get("new BigDecimal(0.1)").toString()).isNotEqualTo("0.1");
        assertThat(result.get("new BigDecimal(\"0.1\")")).isEqualTo("0.1");
        assertThat(result.get("BigDecimal.valueOf(0.1)")).isEqualTo("0.1");
    }
}

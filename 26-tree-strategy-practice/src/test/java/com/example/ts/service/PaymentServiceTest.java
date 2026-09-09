package com.example.ts.service;

import com.example.ts.common.BusinessException;
import com.example.ts.dto.PayRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付策略测试
 */
@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    void testAlipay() {
        PayRequestDTO dto = build("ALIPAY", "100.00");
        BigDecimal actual = paymentService.pay(dto);
        // 100 * 0.006 = 0.6，实付 100.60
        assertEquals(new BigDecimal("100.60"), actual);
    }

    @Test
    void testWechat() {
        PayRequestDTO dto = build("WECHAT", "100.00");
        BigDecimal actual = paymentService.pay(dto);
        // 100 * 0.0038 = 0.38，实付 100.38
        assertEquals(new BigDecimal("100.38"), actual);
    }

    @Test
    void testBalance() {
        PayRequestDTO dto = build("BALANCE", "50.00");
        BigDecimal actual = paymentService.pay(dto);
        assertEquals(new BigDecimal("50.00"), actual);
    }

    @Test
    void testUnsupportedType() {
        PayRequestDTO dto = build("CASH", "10.00");
        assertThrows(BusinessException.class, () -> paymentService.pay(dto));
    }

    private PayRequestDTO build(String type, String amount) {
        PayRequestDTO dto = new PayRequestDTO();
        dto.setPayType(type);
        dto.setOrderNo("ORDER_20260827001");
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }
}

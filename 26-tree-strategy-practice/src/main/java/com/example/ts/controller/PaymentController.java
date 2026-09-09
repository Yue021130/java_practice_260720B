package com.example.ts.controller;

import com.example.ts.common.Result;
import com.example.ts.dto.PayRequestDTO;
import com.example.ts.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 支付接口
 */
@RestController
@RequestMapping("/api/pay")
@Tag(name = "策略模式-支付", description = "动态选择支付方式")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "发起支付")
    public Result<BigDecimal> pay(@Validated @RequestBody PayRequestDTO dto) {
        BigDecimal actual = paymentService.pay(dto);
        return Result.ok(actual);
    }
}

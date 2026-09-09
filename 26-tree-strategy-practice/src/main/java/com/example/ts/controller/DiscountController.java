package com.example.ts.controller;

import com.example.ts.common.Result;
import com.example.ts.dto.DiscountRequestDTO;
import com.example.ts.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 折扣接口
 */
@RestController
@RequestMapping("/api/discount")
@Tag(name = "策略模式-折扣", description = "动态选择折扣策略")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping
    @Operation(summary = "计算折扣后金额")
    public Result<BigDecimal> calculate(@Validated @RequestBody DiscountRequestDTO dto) {
        BigDecimal result = discountService.calculate(dto);
        return Result.ok(result);
    }
}

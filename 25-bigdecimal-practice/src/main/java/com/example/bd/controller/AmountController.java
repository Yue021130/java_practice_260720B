package com.example.bd.controller;

import com.example.bd.common.ApiResponse;
import com.example.bd.dto.CalcRequest;
import com.example.bd.service.AmountCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 金额计算 REST 接口。
 */
@RestController
@RequestMapping("/api/amount")
@RequiredArgsConstructor
@Tag(name = "BigDecimal 金额计算", description = "高精度计算、分账、常见坑")
public class AmountController {

    private final AmountCalculator amountCalculator;

    @PostMapping("/calculate")
    @Operation(summary = "订单金额计算")
    public ApiResponse<Map<String, Object>> calculate(@Valid @RequestBody CalcRequest req) {
        return ApiResponse.ok(amountCalculator.calculateOrderAmount(req));
    }

    @GetMapping("/split")
    @Operation(summary = "分账计算")
    public ApiResponse<Map<String, Object>> split(@RequestParam BigDecimal total,
                                                  @RequestParam BigDecimal platformRate,
                                                  @RequestParam BigDecimal merchantRate,
                                                  @RequestParam(defaultValue = "2") int scale) {
        return ApiResponse.ok(amountCalculator.splitAmount(total, platformRate, merchantRate, scale));
    }

    @GetMapping("/pitfalls")
    @Operation(summary = "BigDecimal 常见坑演示")
    public ApiResponse<Map<String, Object>> pitfalls() {
        return ApiResponse.ok(amountCalculator.pitfalls());
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记：BigDecimal")
    public ApiResponse<Map<String, Object>> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "BigDecimal 核心八股");

        Map<String, String> points = new LinkedHashMap<>();
        points.put("构造方式", "优先 BigDecimal.valueOf(double) 或 new BigDecimal(String)，禁止 new BigDecimal(double)。");
        points.put("比较大小", "使用 compareTo；equals 会比较 scale，1.0 不等于 1.00。");
        points.put("除法", "divide 必须指定 scale 和 RoundingMode，否则无限小数抛 ArithmeticException。");
        points.put("精度设置", "setScale(int, RoundingMode) 显式设置，HALF_UP 是四舍五入。");
        points.put("不可变性", "BigDecimal 不可变，运算后必须接收返回值。");
        points.put("金额单位", "存储用分避免浮点，或用元精确到 2 位小数。");
        points.put("税率语义", "taxRate 为百分比数值，例如 6 表示 6%，后端会自动除以 100。");
        points.put("分账安全", "platformRate + merchantRate 必须 <= 1，避免剩余金额为负。");
        result.put("points", points);

        return ApiResponse.ok(result);
    }
}

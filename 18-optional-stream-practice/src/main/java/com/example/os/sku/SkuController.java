package com.example.os.sku;

import com.example.os.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 05 SKU 最优价格接口。
 */
@RestController
@RequestMapping("/api/sku")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @GetMapping("/best-price")
    @Operation(summary = "SKU 最优价格", description = "查询商品的有效 SKU 最低价、最高价及最优 SKU 详情。")
    public ApiResponse<Map<String, Object>> bestPrice(
            @Parameter(description = "商品 ID", example = "1")
            @RequestParam Long productId) {
        return ApiResponse.ok(skuService.bestPrice(productId));
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(skuService.explain());
    }
}

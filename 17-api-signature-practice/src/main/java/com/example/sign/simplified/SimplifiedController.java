package com.example.sign.simplified;

import com.example.sign.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 08. 简化版方案：appid + timestamp + nonce + uri + sorted_params。
 */
@RestController
@RequestMapping("/api/simplified")
@RequiredArgsConstructor
@Tag(name = "08. 简化版方案", description = "业务简单时用 5 要素简化签名")
public class SimplifiedController {

    private final SimplifiedService service;

    @GetMapping("/demo")
    @Operation(summary = "简化签名演示", description = "appid+timestamp+nonce+uri+排序参数 拼串 → HMAC-SHA256")
    public ApiResponse<Map<String, Object>> demo(@RequestParam(defaultValue = "/api/v1/order/query") String uri,
                                                 @RequestParam(defaultValue = "orderNo=20240701001&page=1") String params) {
        return ApiResponse.success(service.demo(uri, params));
    }

    @GetMapping("/explain")
    @Operation(summary = "简化版速记（八股）", description = "什么时候能简化 / 简化版与标准版的取舍")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

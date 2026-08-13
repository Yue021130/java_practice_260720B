package com.example.sign.principle;

import com.example.sign.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 01. 核心原理：appid / appkey / 签名三要素与鉴权流程。
 */
@RestController
@RequestMapping("/api/principle")
@RequiredArgsConstructor
@Tag(name = "01. 核心原理", description = "appid / appkey / 签名三要素 / 鉴权流程 6 步")
public class PrincipleController {

    private final PrincipleService service;

    @GetMapping("/elements")
    @Operation(summary = "三要素速记", description = "appid 可公开 / appkey 绝不传输 / 签名是 HMAC-SHA256")
    public ApiResponse<Map<String, Object>> elements() {
        return ApiResponse.success(service.elements());
    }

    @GetMapping("/flow")
    @Operation(summary = "鉴权流程 6 步", description = "组装待签串 → 计算签名 → 发请求 → 查 appkey → 重算 → 比对")
    public ApiResponse<Map<String, Object>> flow() {
        return ApiResponse.success(service.flow());
    }

    @GetMapping("/vs-apikey")
    @Operation(summary = "签名 vs 简单 API Key", description = "为什么不能直接传 key：抓包即泄露 / 重放 / 无完整性")
    public ApiResponse<Map<String, Object>> vsApiKey() {
        return ApiResponse.success(service.vsApiKey());
    }

    @GetMapping("/explain")
    @Operation(summary = "原理速记（八股）", description = "为什么大厂都选 HMAC-SHA256")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

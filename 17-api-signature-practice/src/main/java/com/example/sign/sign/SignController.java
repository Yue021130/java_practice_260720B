package com.example.sign.sign;

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
 * 02. 签名计算：Canonical String 构建 + HMAC-SHA256 计算演示。
 */
@RestController
@RequestMapping("/api/sign")
@RequiredArgsConstructor
@Tag(name = "02. 签名计算", description = "Canonical String 9 字段 / HMAC-SHA256")
public class SignController {

    private final SignService service;

    @GetMapping("/compute")
    @Operation(summary = "完整签名计算", description = "按 9 字段拼 Canonical String 并算出 HMAC-SHA256 签名")
    public ApiResponse<Map<String, Object>> compute(@RequestParam(defaultValue = "GET") String method,
                                                    @RequestParam(defaultValue = "/api/v1/users") String uri,
                                                    @RequestParam(defaultValue = "page=1&size=20&name=zhang") String query,
                                                    @RequestParam(defaultValue = "") String body) {
        return ApiResponse.success(service.compute(method, uri, query, body));
    }

    @GetMapping("/canonical")
    @Operation(summary = "Canonical String 9 字段拆解", description = "每个字段怎么来、怎么拼")
    public ApiResponse<Map<String, Object>> canonical() {
        return ApiResponse.success(service.canonical());
    }

    @GetMapping("/verify-manual")
    @Operation(summary = "手工验签（对照）", description = "用返回的签名去反向验证待签串，前端可自行复算")
    public ApiResponse<Map<String, Object>> verifyManual(@RequestParam String appKey,
                                                         @RequestParam String toSign,
                                                         @RequestParam String signature) {
        return ApiResponse.success(service.verifyManual(appKey, toSign, signature));
    }

    @GetMapping("/explain")
    @Operation(summary = "签名算法速记（八股）", description = "为什么按固定顺序 / 空字段怎么处理")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

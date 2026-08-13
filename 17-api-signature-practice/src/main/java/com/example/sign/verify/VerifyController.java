package com.example.sign.verify;

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
 * 03. 服务端验签：正确签名通过 / 篡改任一字段签名失败。
 */
@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
@Tag(name = "03. 服务端验签", description = "重算比对 / 篡改体、时间戳、uri、query 任一字段都失败")
public class VerifyController {

    private final VerifyService service;

    @GetMapping("/demo")
    @Operation(summary = "验签全流程演示", description = "模拟客户端生成签名 → 服务端重算比对；可篡改某字段看失败")
    public ApiResponse<Map<String, Object>> demo(@RequestParam(defaultValue = "none") String tamper) {
        return ApiResponse.success(service.demo(tamper));
    }

    @GetMapping("/explain")
    @Operation(summary = "验签逻辑速记（八股）", description = "提取 → 时间戳 → nonce → appkey → 重算比对")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

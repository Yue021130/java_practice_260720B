package com.example.sign.nonce;

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
 * 05. 防重放-nonce：重复使用拒绝（内存模拟 Redis SETNX + TTL）。
 */
@RestController
@RequestMapping("/api/nonce")
@RequiredArgsConstructor
@Tag(name = "05. 防重放-nonce", description = "SETNX + TTL 语义 / 同一 nonce 第二次被拒")
public class NonceController {

    private final NonceService service;

    @GetMapping("/demo")
    @Operation(summary = "nonce 去重演示", description = "第一次占用成功，用同一个 nonce 再试 → 拒绝")
    public ApiResponse<Map<String, Object>> demo(@RequestParam String nonce) {
        return ApiResponse.success(service.demo(nonce));
    }

    @GetMapping("/explain")
    @Operation(summary = "nonce 防重放速记（八股）", description = "SETNX / TTL / 为什么必须服务端记忆")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

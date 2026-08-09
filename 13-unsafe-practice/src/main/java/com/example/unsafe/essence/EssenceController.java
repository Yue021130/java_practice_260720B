package com.example.unsafe.essence;

import com.example.unsafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 08. 危险与本质实验接口。
 */
@RestController
@RequestMapping("/api/essence")
@RequiredArgsConstructor
@Tag(name = "08. 危险与本质", description = "四大风险 / 本质 / JDK 演变与 VarHandle / 谁在用")
public class EssenceController {

    private final EssenceService service;

    @GetMapping("/risks")
    @Operation(summary = "四大风险", description = "越界崩溃 / 堆外泄漏 / 破坏封装 / 不可移植（只展示代码形态，不真正执行）")
    public ApiResponse<Map<String, Object>> risks() {
        return ApiResponse.success(service.risks());
    }

    @GetMapping("/essence")
    @Operation(summary = "本质", description = "Unsafe 是什么、为什么存在、绕过了哪些安全防线、怎么理解它")
    public ApiResponse<Map<String, Object>> essence() {
        return ApiResponse.success(service.essence());
    }

    @GetMapping("/evolution")
    @Operation(summary = "JDK 演变与 VarHandle", description = "从 JDK 8 到模块化到 JEP 193 VarHandle，Unsafe 的未来")
    public ApiResponse<Map<String, Object>> evolution() {
        return ApiResponse.success(service.evolution());
    }

    @GetMapping("/whouses")
    @Operation(summary = "谁在用 Unsafe", description = "JUC / Netty / Kafka / Cassandra / 序列化框架的落地场景")
    public ApiResponse<Map<String, Object>> whoUses() {
        return ApiResponse.success(service.whoUses());
    }
}

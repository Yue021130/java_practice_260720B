package com.example.cache.basic;

import com.example.cache.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 01. 快速开始实验接口。
 */
@RestController
@RequestMapping("/api/basic")
@RequiredArgsConstructor
@Tag(name = "01. 快速开始", description = "Caffeine 概览 / 手动 Cache / LoadingCache 自动加载")
public class BasicController {

    private final BasicService service;

    @GetMapping("/cache-demo")
    @Operation(summary = "手动 Cache 全流程", description = "miss → 查库 → put → hit → invalidate，对比耗时")
    public ApiResponse<Map<String, Object>> cacheDemo(@RequestParam(defaultValue = "1") int id) {
        return ApiResponse.success(service.cacheDemo(id));
    }

    @GetMapping("/loading")
    @Operation(summary = "LoadingCache 自动加载", description = "get 未命中自动走 CacheLoader，第二次纯内存")
    public ApiResponse<Map<String, Object>> loading(@RequestParam(defaultValue = "1") int id) {
        return ApiResponse.success(service.loading(id));
    }

    @GetMapping("/info")
    @Operation(summary = "核心概念速记", description = "Caffeine 是什么 / 为什么用它 / 与 Guava、Redis 的对比")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.success(service.info());
    }
}

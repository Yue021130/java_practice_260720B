package com.example.cache.preheat;

import com.example.cache.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 05. 缓存预热实验接口（重点场景）。
 */
@RestController
@RequestMapping("/api/preheat")
@RequiredArgsConstructor
@Tag(name = "05. 缓存预热", description = "启动自动预热 / 手动触发 / 状态机 / 预热前后命中率对比")
public class PreheatController {

    private final CachePreheatService service;

    @GetMapping("/status")
    @Operation(summary = "预热状态", description = "PENDING / RUNNING / SUCCESS / FAILED，含 key 数与耗时")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success(service.status());
    }

    @PostMapping("/warm")
    @Operation(summary = "手动触发预热", description = "重复调用幂等：预热中忽略；成功后再次触发会重新预热")
    public ApiResponse<Map<String, Object>> warm() {
        return ApiResponse.success(service.warm());
    }

    @GetMapping("/stats")
    @Operation(summary = "预热收益对比", description = "对热门 key 跑探测读，对比预热前命中率与预热后命中率")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(service.stats());
    }

    @GetMapping("/config")
    @Operation(summary = "预热配置", description = "开关 / key 数 / 批次 / 容量 / 过期时间")
    public ApiResponse<Map<String, Object>> config() {
        return ApiResponse.success(service.config());
    }

    @GetMapping("/explain")
    @Operation(summary = "预热速记（八股）", description = "为什么预热 / 静态与动态 / 时机与失败兜底")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

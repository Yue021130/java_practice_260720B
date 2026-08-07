package com.example.threadpooladvanced.controller;

import com.example.threadpooladvanced.common.ApiResponse;
import com.example.threadpooladvanced.dto.*;
import com.example.threadpooladvanced.service.ThreadPoolExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 线程池基础操作：提交任务、创建自定义池、监控、关闭。
 */
@Tag(name = "01. 线程池基础", description = "预定义池提交、自定义创建、指标监控、优雅关闭")
@RestController
@RequestMapping("/api/pool")
public class PoolController {

    @Autowired
    private ThreadPoolExperimentService experimentService;

    @Operation(summary = "所有线程池实时指标")
    @GetMapping("/metrics")
    public ApiResponse<List<PoolMetricsDto>> metrics() {
        return ApiResponse.ok(experimentService.getAllMetrics());
    }

    @Operation(summary = "指定线程池实时指标")
    @GetMapping("/{poolId}/metrics")
    public ApiResponse<PoolMetricsDto> metricsById(@PathVariable String poolId) {
        return ApiResponse.ok(experimentService.getMetrics(poolId));
    }

    @Operation(summary = "向预定义池提交任务", description = "poolId 可选 cpuPool / ioPool / tinyPool")
    @PostMapping("/predefined/{poolId}/submit")
    public ApiResponse<PoolMetricsDto> submitPredefined(
            @PathVariable String poolId,
            @Valid @RequestBody SubmitTaskRequest request) {
        return ApiResponse.ok(experimentService.submitToPredefined(poolId, request));
    }

    @Operation(summary = "创建自定义线程池")
    @PostMapping("/custom/create")
    public ApiResponse<PoolMetricsDto> createCustom(@Valid @RequestBody CustomPoolRequest request) {
        return ApiResponse.ok(experimentService.createCustomPool(request));
    }

    @Operation(summary = "向自定义线程池提交任务")
    @PostMapping("/custom/{poolId}/submit")
    public ApiResponse<PoolMetricsDto> submitCustom(
            @PathVariable String poolId,
            @Valid @RequestBody SubmitTaskRequest request) {
        return ApiResponse.ok(experimentService.submitToCustom(poolId, request));
    }

    @Operation(summary = "优雅关闭线程池（等待队列执行完毕）")
    @PostMapping("/{poolId}/shutdown")
    public ApiResponse<ShutdownResultDto> shutdown(@PathVariable String poolId) {
        return ApiResponse.ok(experimentService.shutdownPool(poolId, false));
    }

    @Operation(summary = "立即关闭线程池并返回未执行任务")
    @PostMapping("/{poolId}/shutdownNow")
    public ApiResponse<ShutdownResultDto> shutdownNow(@PathVariable String poolId) {
        return ApiResponse.ok(experimentService.shutdownPool(poolId, true));
    }
}

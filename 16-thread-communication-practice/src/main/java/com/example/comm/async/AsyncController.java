package com.example.comm.async;

import com.example.comm.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 08. 基于异步结果传递：Future / FutureTask + CompletableFuture。
 */
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Tag(name = "08. 异步结果传递", description = "FutureTask 拿结果 / CompletableFuture 链式编排")
public class AsyncController {

    private final AsyncService service;

    @GetMapping("/future-demo")
    @Operation(summary = "FutureTask 拿结果", description = "跨线程传递返回值，get() 阻塞等待计算完成")
    public ApiResponse<Map<String, Object>> futureDemo(@RequestParam(defaultValue = "80") int taskMs) {
        return ApiResponse.success(service.futureDemo(taskMs));
    }

    @GetMapping("/cf-demo")
    @Operation(summary = "CompletableFuture 链式编排", description = "supplyAsync → thenApply → thenAccept 依赖传递")
    public ApiResponse<Map<String, Object>> cfDemo(@RequestParam(defaultValue = "50") int taskMs) {
        return ApiResponse.success(service.cfDemo(taskMs));
    }

    @GetMapping("/cf-combine")
    @Operation(summary = "allOf / anyOf 组合", description = "等全部完成 / 任一先完成 / 异常兜底")
    public ApiResponse<Map<String, Object>> cfCombine(@RequestParam(defaultValue = "3") int tasks) {
        return ApiResponse.success(service.cfCombine(tasks));
    }

    @GetMapping("/explain")
    @Operation(summary = "异步结果速记（八股）", description = "Future 局限 vs CompletableFuture / 线程池选择")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

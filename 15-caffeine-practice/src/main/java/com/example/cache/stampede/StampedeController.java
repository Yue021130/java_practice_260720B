package com.example.cache.stampede;

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
 * 06. 穿透/击穿/雪崩实验接口。
 */
@RestController
@RequestMapping("/api/stampede")
@RequiredArgsConstructor
@Tag(name = "06. 穿透/击穿/雪崩", description = "空值缓存防穿透 / 击穿现场 / 单飞 / 三大问题速记")
public class StampedeController {

    private final StampedeService service;

    @GetMapping("/overview")
    @Operation(summary = "三大问题速记（八股）", description = "穿透/击穿/雪崩 的现象与应对")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(service.overview());
    }

    @GetMapping("/null-demo")
    @Operation(summary = "空值缓存防穿透", description = "不存在的 key：不缓存 null 全部打库 vs 空值缓存只打一次")
    public ApiResponse<Map<String, Object>> nullDemo(@RequestParam(defaultValue = "50") int times) {
        return ApiResponse.success(service.nullDemo(times));
    }

    @GetMapping("/stampede-demo")
    @Operation(summary = "击穿现场（无保护）", description = "热点 key 过期瞬间 N 线程同时打到 DB，数一数查了几次库")
    public ApiResponse<Map<String, Object>> stampedeDemo(@RequestParam(defaultValue = "20") int threads) {
        return ApiResponse.success(service.stampedeDemo(threads));
    }

    @GetMapping("/singleflight")
    @Operation(summary = "单飞保护（只查一次）", description = "同一 key 的并发加载合并成 1 次，其余线程等待同一结果")
    public ApiResponse<Map<String, Object>> singleflight(@RequestParam(defaultValue = "20") int threads) {
        return ApiResponse.success(service.singleflight(threads));
    }

    @GetMapping("/explain")
    @Operation(summary = "单飞与逻辑过期速记（八股）", description = "单飞实现 / 逻辑过期 / 死锁注意点")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

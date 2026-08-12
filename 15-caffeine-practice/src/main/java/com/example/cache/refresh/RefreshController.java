package com.example.cache.refresh;

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
 * 03. 刷新与异步实验接口。
 */
@RestController
@RequestMapping("/api/refresh")
@RequiredArgsConstructor
@Tag(name = "03. 刷新与异步", description = "refreshAfterWrite 异步刷新 / AsyncCache 异步加载 / 速记")
public class RefreshController {

    private final RefreshService service;

    @GetMapping("/refresh-demo")
    @Operation(summary = "定时刷新演示", description = "超过刷新间隔后读不阻塞、旧值先回、后台异步刷新")
    public ApiResponse<Map<String, Object>> refreshDemo(@RequestParam(defaultValue = "0") long waitMs) {
        return ApiResponse.success(service.refreshDemo(waitMs));
    }

    @GetMapping("/async-demo")
    @Operation(summary = "异步加载演示", description = "AsyncCache 返回 CompletableFuture，加载在线程池完成")
    public ApiResponse<Map<String, Object>> asyncDemo(@RequestParam(defaultValue = "1") int id) {
        return ApiResponse.success(service.asyncDemo(id));
    }

    @GetMapping("/explain")
    @Operation(summary = "刷新与异步速记（八股）", description = "refresh vs expire / 黄金组合 / AsyncCache 注意点")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

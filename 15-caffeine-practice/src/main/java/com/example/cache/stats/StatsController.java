package com.example.cache.stats;

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
 * 04. 统计与监控实验接口。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "04. 统计与监控", description = "命中率/淘汰数/加载耗时采样 / 指标速记")
public class StatsController {

    private final StatsService service;

    @GetMapping("/demo")
    @Operation(summary = "统计采样", description = "跑 N 次访问，返回命中率/查库次数/加载耗时等指标")
    public ApiResponse<Map<String, Object>> demo(@RequestParam(defaultValue = "500") int accesses) {
        return ApiResponse.success(service.demo(accesses));
    }

    @GetMapping("/explain")
    @Operation(summary = "统计指标速记（八股）", description = "recordStats / 各指标含义 / 监控与告警")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

package com.example.cache.eviction;

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
 * 02. 淘汰策略实验接口。
 */
@RestController
@RequestMapping("/api/eviction")
@RequiredArgsConstructor
@Tag(name = "02. 淘汰策略", description = "容量淘汰 / 时间淘汰（write vs access）/ 策略速记")
public class EvictionController {

    private final EvictionService service;

    @GetMapping("/size-demo")
    @Operation(summary = "容量淘汰演示", description = "maximumSize=5，放进 N 个看淘汰数量与存活 key")
    public ApiResponse<Map<String, Object>> sizeDemo(@RequestParam(defaultValue = "12") int count) {
        return ApiResponse.success(service.sizeDemo(count));
    }

    @GetMapping("/expire-demo")
    @Operation(summary = "时间淘汰演示", description = "expireAfterWrite vs expireAfterAccess：一个读不续命、一个读续命")
    public ApiResponse<Map<String, Object>> expireDemo(
            @RequestParam(defaultValue = "write") String type,
            @RequestParam(defaultValue = "150") long durationMs) {
        return ApiResponse.success(service.expireDemo(type, durationMs));
    }

    @GetMapping("/explain")
    @Operation(summary = "淘汰策略速记（八股）", description = "容量 / 时间 / 引用三类策略与适用场景")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

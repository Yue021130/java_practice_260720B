package com.example.cache.pitfall;

import com.example.cache.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 10. 常见坑与调优实验接口。
 */
@RestController
@RequestMapping("/api/pitfall")
@RequiredArgsConstructor
@Tag(name = "10. 常见坑与调优", description = "10 个高频坑 / SpEL key 陷阱 / 调优要点")
public class PitfallController {

    private final PitfallService service;

    @GetMapping("/list")
    @Operation(summary = "10 个高频坑清单", description = "每个坑：现象 → 原因 → 解法")
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/key-demo")
    @Operation(summary = "SpEL key 陷阱现场", description = "等价对象因 toString 不同导致缓存永远 miss")
    public ApiResponse<Map<String, Object>> keyDemo() {
        return ApiResponse.success(service.keyDemo());
    }

    @GetMapping("/tuning")
    @Operation(summary = "调优要点（八股）", description = "容量 / TTL / 预热 / 监控 / 大促三件套")
    public ApiResponse<Map<String, Object>> tuning() {
        return ApiResponse.success(service.tuning());
    }
}

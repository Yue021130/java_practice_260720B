package com.example.cache.consistency;

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
 * 09. 缓存一致性实验接口。
 */
@RestController
@RequestMapping("/api/consistency")
@RequiredArgsConstructor
@Tag(name = "09. 缓存一致性", description = "Cache Aside 现场 / 双删 / 一致性模式与解法")
public class ConsistencyController {

    private final ConsistencyService service;

    @GetMapping("/aside-demo")
    @Operation(summary = "Cache Aside 现场", description = "只更库不删缓存→脏数据；更库+删缓存→自愈")
    public ApiResponse<Map<String, Object>> asideDemo(@RequestParam(defaultValue = "2") int id) {
        return ApiResponse.success(service.asideDemo(id));
    }

    @GetMapping("/double-delete-demo")
    @Operation(summary = "双删演示", description = "写前删 + 写后删，压掉读回填的竞态窗口")
    public ApiResponse<Map<String, Object>> doubleDeleteDemo(@RequestParam(defaultValue = "3") int id) {
        return ApiResponse.success(service.doubleDeleteDemo(id));
    }

    @GetMapping("/patterns")
    @Operation(summary = "一致性模式对比", description = "Cache Aside / Read Through / Write Through / Write Behind")
    public ApiResponse<Map<String, Object>> patterns() {
        return ApiResponse.success(service.patterns());
    }

    @GetMapping("/explain")
    @Operation(summary = "一致性速记（八股）", description = "为什么删缓存不更新 / 不一致来源 / 双删与 binlog")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

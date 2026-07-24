package com.example.cg.realworld;

import com.example.cg.common.ApiResponse;
import com.example.cg.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 集合与泛型综合实战 REST 接口。
 */
@Tag(name = "综合实战", description = "LRU / TopK / groupBy / Comparator 多字段排序")
@RestController
@RequestMapping("/api/realworld")
@RequiredArgsConstructor
public class RealWorldController {

    private final RealWorldScenarioService realWorldScenarioService;

    @Operation(summary = "LRU 缓存", description = "LinkedHashMap 实现带容量与过期时间的缓存")
    @PostMapping("/lru-cache")
    public ApiResponse<ScenarioResult> lruCache(
            @Parameter(description = "缓存容量", example = "3") @RequestParam(defaultValue = "3") int capacity) {
        if (capacity < 2 || capacity > 10) {
            return ApiResponse.error(400, "capacity 必须在 2~10 之间");
        }
        return ApiResponse.success(realWorldScenarioService.lruCache(capacity));
    }

    @Operation(summary = "TopK 热词统计", description = "HashMap 词频 + PriorityQueue 取 TopK")
    @PostMapping("/topk-words")
    public ApiResponse<ScenarioResult> topkWords(
            @Parameter(description = "前 K 个", example = "5") @RequestParam(defaultValue = "5") int k) {
        if (k < 1 || k > 20) {
            return ApiResponse.error(400, "k 必须在 1~20 之间");
        }
        return ApiResponse.success(realWorldScenarioService.topkWords(k));
    }

    @Operation(summary = "groupBy 订单", description = "按用户/状态分组聚合金额")
    @PostMapping("/groupby")
    public ApiResponse<ScenarioResult> groupby() {
        return ApiResponse.success(realWorldScenarioService.groupby());
    }

    @Operation(summary = "通用排序工具", description = "Comparator.comparing / thenComparing / nullsFirst 多字段排序")
    @PostMapping("/comparator-sort")
    public ApiResponse<ScenarioResult> comparatorSort() {
        return ApiResponse.success(realWorldScenarioService.comparatorSort());
    }
}

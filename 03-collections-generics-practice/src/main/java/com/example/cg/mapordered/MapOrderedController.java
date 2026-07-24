package com.example.cg.mapordered;

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
 * 有序/排序 Map 场景 REST 接口。
 *
 * 覆盖 LinkedHashMap LRU、TreeMap 排序、WeakHashMap。
 */
@Tag(name = "有序 Map 场景", description = "LinkedHashMap / TreeMap / WeakHashMap")
@RestController
@RequestMapping("/api/mapordered")
@RequiredArgsConstructor
public class MapOrderedController {

    private final MapOrderedScenarioService mapOrderedScenarioService;

    @Operation(summary = "LinkedHashMap LRU 缓存", description = "accessOrder=true 实现最近最少使用淘汰")
    @PostMapping("/linkedhashmap-lru")
    public ApiResponse<ScenarioResult> linkedhashmapLru(
            @Parameter(description = "缓存容量", example = "5") @RequestParam(defaultValue = "5") int capacity) {
        if (capacity < 2 || capacity > 20) {
            return ApiResponse.error(400, "capacity 必须在 2~20 之间");
        }
        return ApiResponse.success(mapOrderedScenarioService.linkedhashmapLru(capacity));
    }

    @Operation(summary = "TreeMap 排序与范围查询", description = "自然排序 / 自定义 Comparator / subMap")
    @PostMapping("/treemap-sort")
    public ApiResponse<ScenarioResult> treemapSort() {
        return ApiResponse.success(mapOrderedScenarioService.treemapSort());
    }

    @Operation(summary = "WeakHashMap 弱引用缓存", description = "key 被 GC 后 Entry 自动清理")
    @PostMapping("/weakhashmap-cache")
    public ApiResponse<ScenarioResult> weakhashmapCache() {
        return ApiResponse.success(mapOrderedScenarioService.weakhashmapCache());
    }
}

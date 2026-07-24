package com.example.cg.maphash;

import com.example.cg.common.ApiResponse;
import com.example.cg.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HashMap 原理场景 REST 接口。
 *
 * 覆盖扰动函数、容量为 2 的幂、负载因子、key 不可变、HashMap vs Hashtable vs CHM。
 */
@Tag(name = "HashMap 原理", description = "扰动函数 / 树化 / 扩容 / key 不可变 / Map 对比")
@RestController
@RequestMapping("/api/maphash")
@RequiredArgsConstructor
public class MapHashController {

    private final MapHashScenarioService mapHashScenarioService;

    @Operation(summary = "HashMap 内部机制", description = "演示 hash 扰动、容量 2 的幂、index 计算、扩容")
    @PostMapping("/hashmap-internals")
    public ApiResponse<ScenarioResult> hashmapInternals() {
        return ApiResponse.success(mapHashScenarioService.hashmapInternals());
    }

    @Operation(summary = "key 被修改后丢失", description = "可变对象作为 key，put 后修改字段会导致 get 失败")
    @PostMapping("/key-mutation")
    public ApiResponse<ScenarioResult> keyMutation() {
        return ApiResponse.success(mapHashScenarioService.keyMutation());
    }

    @Operation(summary = "HashMap vs Hashtable vs ConcurrentHashMap", description = "线程安全性与 null 支持对比")
    @PostMapping("/map-compare")
    public ApiResponse<ScenarioResult> mapCompare() {
        return ApiResponse.success(mapHashScenarioService.mapCompare());
    }
}

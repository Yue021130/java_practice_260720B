package com.example.cg.utils;

import com.example.cg.common.ApiResponse;
import com.example.cg.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Collections / Arrays 工具类场景 REST 接口。
 */
@Tag(name = "工具类场景", description = "Collections / Arrays 常用工具与陷阱")
@RestController
@RequestMapping("/api/utils")
@RequiredArgsConstructor
public class UtilsController {

    private final UtilsScenarioService utilsScenarioService;

    @Operation(summary = "sort + binarySearch", description = "Collections.sort 与 Comparator，binarySearch 前必须先排序")
    @PostMapping("/sort-binarysearch")
    public ApiResponse<ScenarioResult> sortBinarysearch() {
        return ApiResponse.success(utilsScenarioService.sortBinarysearch());
    }

    @Operation(summary = "synchronized / unmodifiable 包装器", description = "线程安全视图与只读视图的本质")
    @PostMapping("/synchronized-unmodifiable")
    public ApiResponse<ScenarioResult> synchronizedUnmodifiable() {
        return ApiResponse.success(utilsScenarioService.synchronizedUnmodifiable());
    }

    @Operation(summary = "Arrays.asList 陷阱", description = "返回固定大小视图，add/remove 抛异常")
    @PostMapping("/arrays-aslist-trap")
    public ApiResponse<ScenarioResult> arraysAslistTrap() {
        return ApiResponse.success(utilsScenarioService.arraysAslistTrap());
    }

    @Operation(summary = "Collections.shuffle", description = "随机打乱集合，可用 Random 种子复现")
    @PostMapping("/shuffle")
    public ApiResponse<ScenarioResult> shuffle() {
        return ApiResponse.success(utilsScenarioService.shuffle());
    }
}

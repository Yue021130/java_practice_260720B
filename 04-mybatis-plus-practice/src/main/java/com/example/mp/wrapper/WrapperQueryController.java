package com.example.mp.wrapper;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 条件构造器 Wrapper 查询演示。
 */
@RestController
@RequestMapping("/api/wrapper")
@RequiredArgsConstructor
@Tag(name = "条件构造器", description = "QueryWrapper / LambdaQueryWrapper / 嵌套条件")
public class WrapperQueryController {

    private final WrapperQueryService wrapperQueryService;

    @PostMapping("/eq-like")
    @Operation(summary = "等值 + 模糊查询", description = "QueryWrapper.eq / like 用法")
    public ApiResponse<Map<String, Object>> eqLike() {
        return ApiResponse.success(wrapperQueryService.eqLikeDemo());
    }

    @PostMapping("/between-order")
    @Operation(summary = "范围 + 排序", description = "between / orderByAsc / orderByDesc")
    public ApiResponse<Map<String, Object>> betweenOrder() {
        return ApiResponse.success(wrapperQueryService.betweenOrderDemo());
    }

    @PostMapping("/lambda")
    @Operation(summary = "Lambda 条件构造器", description = "LambdaQueryWrapper 类型安全，避免硬编码字段名")
    public ApiResponse<Map<String, Object>> lambda() {
        return ApiResponse.success(wrapperQueryService.lambdaDemo());
    }

    @PostMapping("/nested")
    @Operation(summary = "嵌套条件", description = "and / or 组合复杂查询条件")
    public ApiResponse<Map<String, Object>> nested() {
        return ApiResponse.success(wrapperQueryService.nestedDemo());
    }
}

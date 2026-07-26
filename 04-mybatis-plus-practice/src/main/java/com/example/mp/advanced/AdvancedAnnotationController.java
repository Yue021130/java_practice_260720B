package com.example.mp.advanced;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 高级注解演示：逻辑删除、乐观锁、自动填充。
 */
@RestController
@RequestMapping("/api/advanced")
@RequiredArgsConstructor
@Tag(name = "高级注解", description = "@TableLogic / @Version / @TableField(fill)")
public class AdvancedAnnotationController {

    private final AdvancedAnnotationService advancedAnnotationService;

    @PostMapping("/logic-delete")
    @Operation(summary = "逻辑删除", description = "@TableLogic 标记 deleted 字段，删除时只更新标记")
    public ApiResponse<Map<String, Object>> logicDelete() {
        return ApiResponse.success(advancedAnnotationService.logicDeleteDemo());
    }

    @PostMapping("/optimistic-lock")
    @Operation(summary = "乐观锁", description = "@Version 更新时 version 自增，防止并发覆盖")
    public ApiResponse<Map<String, Object>> optimisticLock() {
        return ApiResponse.success(advancedAnnotationService.optimisticLockDemo());
    }

    @PostMapping("/auto-fill")
    @Operation(summary = "自动填充", description = "MetaObjectHandler 自动填充 createTime / updateTime")
    public ApiResponse<Map<String, Object>> autoFill() {
        return ApiResponse.success(advancedAnnotationService.autoFillDemo());
    }
}

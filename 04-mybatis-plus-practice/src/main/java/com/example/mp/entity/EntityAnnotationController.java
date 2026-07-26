package com.example.mp.entity;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 实体类注解演示：@TableName / @TableId / @TableField。
 */
@RestController
@RequestMapping("/api/entity")
@RequiredArgsConstructor
@Tag(name = "实体类注解", description = "@TableName / @TableId / @TableField")
public class EntityAnnotationController {

    private final EntityAnnotationService entityAnnotationService;

    @PostMapping("/table-name")
    @Operation(summary = "@TableName 表名映射", description = "演示实体类 User 映射到 t_user 表")
    public ApiResponse<Map<String, Object>> tableName() {
        return ApiResponse.success(entityAnnotationService.tableNameDemo());
    }

    @PostMapping("/table-id")
    @Operation(summary = "@TableId 主键策略", description = "演示 ASSIGN_ID 雪花 ID 与 AUTO 自增策略对比")
    public ApiResponse<Map<String, Object>> tableId() {
        return ApiResponse.success(entityAnnotationService.tableIdDemo());
    }

    @PostMapping("/table-field")
    @Operation(summary = "@TableField 字段映射", description = "演示字段别名、排除非持久化字段、exist=false")
    public ApiResponse<Map<String, Object>> tableField() {
        return ApiResponse.success(entityAnnotationService.tableFieldDemo());
    }
}

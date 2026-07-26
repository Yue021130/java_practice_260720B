package com.example.mp.extension;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MyBatis-Plus 扩展能力演示：TypeHandler、ActiveRecord、动态表名、批量插入注入器、链式 Wrapper、复杂 Wrapper 等。
 */
@RestController
@RequestMapping("/api/extension")
@RequiredArgsConstructor
@Tag(name = "扩展实战", description = "TypeHandler / ActiveRecord / 动态表名 / InsertBatchSomeColumn / 链式 Wrapper / 复杂 Wrapper / selectMaps / Wrapper 更新删除")
public class ExtensionController {

    private final ExtensionService extensionService;

    @PostMapping("/type-handler")
    @Operation(summary = "TypeHandler 自定义类型转换", description = "Map 字段以 JSON 字符串存储到数据库")
    public ApiResponse<Map<String, Object>> typeHandler() {
        return ApiResponse.success(extensionService.typeHandlerDemo());
    }

    @PostMapping("/active-record")
    @Operation(summary = "ActiveRecord 模式", description = "实体继承 Model<T> 后直接调用 insert/selectById")
    public ApiResponse<Map<String, Object>> activeRecord() {
        return ApiResponse.success(extensionService.activeRecordDemo());
    }

    @PostMapping("/dynamic-table-name")
    @Operation(summary = "动态表名", description = "DynamicTableNameInnerInterceptor 实现按月分表")
    public ApiResponse<Map<String, Object>> dynamicTableName() {
        return ApiResponse.success(extensionService.dynamicTableNameDemo());
    }

    @PostMapping("/insert-batch-some-column")
    @Operation(summary = "InsertBatchSomeColumn 批量插入", description = "自定义 SQL 注入器实现一条 SQL 批量插入")
    public ApiResponse<Map<String, Object>> insertBatchSomeColumn() {
        return ApiResponse.success(extensionService.insertBatchSomeColumnDemo());
    }

    @PostMapping("/chain-wrapper")
    @Operation(summary = "链式 Wrapper", description = "IService.lambdaQuery()/lambdaUpdate() 一行链式调用")
    public ApiResponse<Map<String, Object>> chainWrapper() {
        return ApiResponse.success(extensionService.chainWrapperDemo());
    }

    @PostMapping("/wrapper-advanced")
    @Operation(summary = "复杂 Wrapper", description = "inSql 子查询与 groupBy having 聚合")
    public ApiResponse<Map<String, Object>> wrapperAdvanced() {
        return ApiResponse.success(extensionService.wrapperAdvancedDemo());
    }

    @PostMapping("/select-maps")
    @Operation(summary = "selectMaps / selectObjs", description = "返回 Map 集合或单列集合")
    public ApiResponse<Map<String, Object>> selectMaps() {
        return ApiResponse.success(extensionService.selectMapsDemo());
    }

    @PostMapping("/wrapper-update-delete")
    @Operation(summary = "Wrapper 更新 / 删除", description = "按条件批量更新和按条件删除")
    public ApiResponse<Map<String, Object>> wrapperUpdateDelete() {
        return ApiResponse.success(extensionService.wrapperUpdateDeleteDemo());
    }
}

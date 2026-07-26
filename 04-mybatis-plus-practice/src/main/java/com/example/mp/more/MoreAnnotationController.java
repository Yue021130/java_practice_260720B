package com.example.mp.more;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 更多 MyBatis-Plus + Spring 注解演示：@KeySequence / @OrderBy / @EnumValue / @InterceptorIgnore / FieldStrategy / IdType / @Order / 自定义注解。
 */
@RestController
@RequestMapping("/api/more")
@RequiredArgsConstructor
@Tag(name = "更多注解", description = "@KeySequence / @OrderBy / @EnumValue / @InterceptorIgnore / FieldStrategy / IdType / @Order / 自定义注解")
public class MoreAnnotationController {

    private final MoreAnnotationService moreAnnotationService;

    @PostMapping("/key-sequence")
    @Operation(summary = "@KeySequence 序列主键", description = "H2 序列生成主键并回填")
    public ApiResponse<Map<String, Object>> keySequence() {
        return ApiResponse.success(moreAnnotationService.keySequenceDemo());
    }

    @PostMapping("/order-by")
    @Operation(summary = "@OrderBy 默认排序", description = "实体字段标注默认排序规则")
    public ApiResponse<Map<String, Object>> orderBy() {
        return ApiResponse.success(moreAnnotationService.orderByDemo());
    }

    @PostMapping("/enum-value")
    @Operation(summary = "@EnumValue 枚举映射", description = "枚举按 code 值持久化，按 desc 展示")
    public ApiResponse<Map<String, Object>> enumValue() {
        return ApiResponse.success(moreAnnotationService.enumValueDemo());
    }

    @PostMapping("/interceptor-ignore")
    @Operation(summary = "@InterceptorIgnore 忽略插件", description = "临时忽略分页插件")
    public ApiResponse<Map<String, Object>> interceptorIgnore() {
        return ApiResponse.success(moreAnnotationService.interceptorIgnoreDemo());
    }

    @PostMapping("/field-select")
    @Operation(summary = "@TableField(select=false)", description = "查询时不返回敏感字段")
    public ApiResponse<Map<String, Object>> fieldSelect() {
        return ApiResponse.success(moreAnnotationService.fieldSelectDemo());
    }

    @PostMapping("/field-condition")
    @Operation(summary = "@TableField(condition)", description = "自定义 WHERE 条件模板")
    public ApiResponse<Map<String, Object>> fieldCondition() {
        return ApiResponse.success(moreAnnotationService.fieldConditionDemo());
    }

    @PostMapping("/field-update")
    @Operation(summary = "@TableField(update)", description = "自定义 SET 片段实现自增")
    public ApiResponse<Map<String, Object>> fieldUpdate() {
        return ApiResponse.success(moreAnnotationService.fieldUpdateDemo());
    }

    @PostMapping("/field-numeric-scale")
    @Operation(summary = "@TableField(numericScale)", description = "指定 DECIMAL 小数位")
    public ApiResponse<Map<String, Object>> fieldNumericScale() {
        return ApiResponse.success(moreAnnotationService.fieldNumericScaleDemo());
    }

    @PostMapping("/id-types")
    @Operation(summary = "IdType 全策略", description = "主键生成策略一览")
    public ApiResponse<Map<String, Object>> idTypes() {
        return ApiResponse.success(moreAnnotationService.idTypesDemo());
    }

    @PostMapping("/field-strategy")
    @Operation(summary = "FieldStrategy", description = "字段插入/更新策略：NOT_NULL / NOT_EMPTY / IGNORED / DEFAULT")
    public ApiResponse<Map<String, Object>> fieldStrategy() {
        return ApiResponse.success(moreAnnotationService.fieldStrategyDemo());
    }

    @PostMapping("/order")
    @Operation(summary = "@Order 执行顺序", description = "多个 CommandLineRunner 按 @Order 顺序执行")
    public ApiResponse<Map<String, Object>> order() {
        return ApiResponse.success(moreAnnotationService.orderDemo());
    }

    @PostMapping("/custom-annotation")
    @Operation(summary = "自定义注解 + AOP", description = "@AutoFillUser 自动填充 createBy / updateBy，多个切面按 @Order 执行")
    public ApiResponse<Map<String, Object>> customAnnotation() {
        return ApiResponse.success(moreAnnotationService.customAnnotationDemo());
    }

    @PostMapping("/accessors")
    @Operation(summary = "@Accessors 链式/fluent/prefix", description = "Lombok @Accessors 的 chain / fluent / prefix 三种用法")
    public ApiResponse<Map<String, Object>> accessors() {
        return ApiResponse.success(moreAnnotationService.accessorsDemo());
    }
}

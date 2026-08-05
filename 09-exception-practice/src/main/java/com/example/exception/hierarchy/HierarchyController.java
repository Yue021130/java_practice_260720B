package com.example.exception.hierarchy;

import com.example.exception.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * 异常体系与分类实验接口。
 */
@RestController
@RequestMapping("/api/hierarchy")
@RequiredArgsConstructor
@Tag(name = "01. 异常体系与分类", description = "Throwable 家谱、checked/unchecked、自定义业务异常")
public class HierarchyController {

    private final HierarchyScenarioService service;

    @GetMapping("/family")
    @Operation(summary = "Throwable 家谱", description = "返回 Error / Exception / RuntimeException 的层级与示例")
    public ApiResponse<Map<String, Object>> family() {
        return ApiResponse.success(service.familyTree());
    }

    @PostMapping("/checked-unchecked")
    @Operation(summary = "Checked vs Unchecked", description = "演示 checked exception 必须声明，unchecked 可隐式抛出")
    public ApiResponse<Map<String, Object>> checkedUnchecked(
            @Parameter(description = "是否触发 checked exception")
            @RequestParam(defaultValue = "true") boolean checked) {
        return ApiResponse.success(service.checkedUnchecked(checked));
    }

    @PostMapping("/custom-exception")
    @Operation(summary = "自定义业务异常", description = "演示继承 RuntimeException 的 BusinessException，携带错误码与 cause")
    public ApiResponse<Map<String, Object>> customException(
            @Parameter(description = "是否抛出带 cause 的业务异常")
            @RequestParam(defaultValue = "true") boolean throwWithCause) {
        return ApiResponse.success(service.customException(throwWithCause));
    }

    @GetMapping("/when-to-use")
    @Operation(summary = "何时用 checked / unchecked", description = "给出 checked 与 unchecked 的选型建议")
    public ApiResponse<Map<String, Object>> whenToUse() {
        return ApiResponse.success(service.whenToUse());
    }
}

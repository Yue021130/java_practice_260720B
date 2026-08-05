package com.example.exception.advanced;

import com.example.exception.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 异常进阶特性实验接口。
 */
@RestController
@RequestMapping("/api/advanced")
@RequiredArgsConstructor
@Tag(name = "04. 异常进阶特性", description = "多 catch、精确重抛、Lambda checked、Stream 异常、Suppressed、性能")
public class AdvancedController {

    private final AdvancedScenarioService service;

    @PostMapping("/multi-catch")
    @Operation(summary = "Java 7 多 catch", description = "同时捕获多个不相关的 checked exception")
    public ApiResponse<Map<String, Object>> multiCatch() {
        return ApiResponse.success(service.multiCatch());
    }

    @PostMapping("/rethrow")
    @Operation(summary = "更精确重抛", description = "编译器推断实际抛出的异常类型")
    public ApiResponse<Map<String, Object>> rethrow() {
        return ApiResponse.success(service.rethrow());
    }

    @PostMapping("/lambda-checked")
    @Operation(summary = "Lambda 受检异常处理", description = "三种处理方式：内部 try-catch / 包装 / 自定义函数式接口")
    public ApiResponse<Map<String, Object>> lambdaChecked() {
        return ApiResponse.success(service.lambdaChecked());
    }

    @PostMapping("/stream-exception")
    @Operation(summary = "Stream 异常短路", description = "Stream 中间操作异常会终止整个流水线")
    public ApiResponse<Map<String, Object>> streamException() {
        return ApiResponse.success(service.streamException());
    }

    @PostMapping("/suppressed")
    @Operation(summary = "Suppressed Exception", description = "try-with-resources 中 close 异常被挂起")
    public ApiResponse<Map<String, Object>> suppressed() throws Exception {
        return ApiResponse.success(service.suppressed());
    }

    @PostMapping("/exception-masking")
    @Operation(summary = "异常屏蔽", description = "catch 中抛新异常不保留 cause 会丢失原始异常")
    public ApiResponse<Map<String, Object>> exceptionMasking() {
        return ApiResponse.success(service.exceptionMasking());
    }

    @PostMapping("/performance")
    @Operation(summary = "异常创建性能开销", description = "fillInStackTrace 是主要开销，可重写优化")
    public ApiResponse<Map<String, Object>> performance() {
        return ApiResponse.success(service.performance());
    }
}

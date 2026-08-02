package com.example.sl.basic;

import com.example.sl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lambda")
@RequiredArgsConstructor
@Tag(name = "Lambda 基础", description = "Lambda 表达式与函数式接口")
public class LambdaController {

    private final LambdaService lambdaService;

    @PostMapping("/functional")
    @Operation(summary = "Lambda 与函数式接口", description = "Predicate / Function / Consumer / Supplier 演示")
    public ApiResponse<Map<String, Object>> functional() {
        return ApiResponse.success(lambdaService.functionalDemo());
    }

    @PostMapping("/method-ref")
    @Operation(summary = "方法引用", description = "静态、实例、构造方法引用演示")
    public ApiResponse<Map<String, Object>> methodRef() {
        return ApiResponse.success(lambdaService.methodRefDemo());
    }
}

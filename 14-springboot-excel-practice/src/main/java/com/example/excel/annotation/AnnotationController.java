package com.example.excel.annotation;

import com.example.excel.common.ApiResponse;
import com.example.excel.support.ExcelWebSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 02. 注解与字段实验接口。
 */
@RestController
@RequestMapping("/api/annotation")
@RequiredArgsConstructor
@Tag(name = "02. 注解与字段", description = "列顺序 / 忽略字段 / 列宽行高 / 数字日期格式 / 注解总表")
public class AnnotationController {

    private final AnnotationService service;

    @GetMapping("/download")
    @Operation(summary = "下载注解演示.xlsx", description = "列顺序按 order 重排，remark 被忽略，月薪带千分位，日期转 yyyy-MM-dd")
    public ResponseEntity<byte[]> download() {
        return ExcelWebSupport.xlsx("注解演示.xlsx", service.exportBytes());
    }

    @GetMapping("/export-demo")
    @Operation(summary = "导出演示（JSON）", description = "输出列顺序 vs 声明顺序 / 忽略字段验证")
    public ApiResponse<Map<String, Object>> exportDemo() {
        return ApiResponse.success(service.exportDemo());
    }

    @PostMapping("/import-demo")
    @Operation(summary = "导入演示（JSON）", description = "读回文件，验证格式注解反向解析、@ExcelIgnore 不赋值")
    public ApiResponse<Map<String, Object>> importDemo() {
        return ApiResponse.success(service.importDemo());
    }

    @GetMapping("/explain")
    @Operation(summary = "注解总表（八股）", description = "全部常用注解的作用与适用场景")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

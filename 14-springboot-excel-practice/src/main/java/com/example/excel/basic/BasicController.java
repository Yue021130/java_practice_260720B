package com.example.excel.basic;

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
 * 01. 快速开始实验接口。
 */
@RestController
@RequestMapping("/api/basic")
@RequiredArgsConstructor
@Tag(name = "01. 快速开始", description = "最简单的导出 / 导入 / 核心 API 速记")
public class BasicController {

    private final BasicService service;

    @GetMapping("/download")
    @Operation(summary = "下载员工名单.xlsx", description = "真实导出：返回 xlsx 二进制流，带中文文件名响应头")
    public ResponseEntity<byte[]> download() {
        return ExcelWebSupport.xlsx("员工名单.xlsx", service.exportBytes());
    }

    @GetMapping("/export-demo")
    @Operation(summary = "导出演示（JSON）", description = "内存导出并返回列数/行数/文件大小/耗时，面板用")
    public ApiResponse<Map<String, Object>> exportDemo() {
        return ApiResponse.success(service.exportDemo());
    }

    @PostMapping("/import-demo")
    @Operation(summary = "导入演示（JSON）", description = "内存生成样本文件再解析回 List，验证注解双向映射")
    public ApiResponse<Map<String, Object>> importDemo() {
        return ApiResponse.success(service.importDemo());
    }

    @GetMapping("/overview")
    @Operation(summary = "核心概念速记", description = "EasyExcel 是什么 / 为什么用它 / 核心 API 两行")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(service.overview());
    }
}

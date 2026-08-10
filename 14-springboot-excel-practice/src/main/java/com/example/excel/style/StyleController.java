package com.example.excel.style;

import com.example.excel.common.ApiResponse;
import com.example.excel.support.ExcelWebSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 03. 样式与格式实验接口。
 */
@RestController
@RequestMapping("/api/style")
@RequiredArgsConstructor
@Tag(name = "03. 样式与格式", description = "表头/内容样式 / 条件高亮 / 合并相同单元格 / 样式机制速记")
public class StyleController {

    private final StyleService service;

    @GetMapping("/download")
    @Operation(summary = "下载部门薪资报表.xlsx", description = "浅蓝表头 + 全边框内容 + 月薪>15000 标红 + 部门自动合并")
    public ResponseEntity<byte[]> download() {
        return ExcelWebSupport.xlsx("部门薪资报表.xlsx", service.exportBytes());
    }

    @GetMapping("/export-demo")
    @Operation(summary = "导出演示（JSON）", description = "说明本文件应用了哪些样式")
    public ApiResponse<Map<String, Object>> exportDemo() {
        return ApiResponse.success(service.exportDemo());
    }

    @GetMapping("/explain")
    @Operation(summary = "样式机制速记（八股）", description = "注解 / 内置策略 / 自定义 Handler 三层 + 合并单元格手段")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

package com.example.excel.template;

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
 * 08. 模板导出实验接口。
 */
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
@Tag(name = "08. 模板导出", description = "模板填充 / 简单填充与列表填充 / 模板设计原则")
public class TemplateController {

    private final TemplateService service;

    @GetMapping("/template-download")
    @Operation(summary = "下载空白模板.xlsx", description = "含 {占位符} 的销售订单模板，可下载后自己用 Excel 改样式")
    public ResponseEntity<byte[]> templateDownload() {
        return ExcelWebSupport.xlsx("销售订单模板.xlsx", service.templateBytes());
    }

    @GetMapping("/fill-download")
    @Operation(summary = "下载填充后的订单.xlsx", description = "简单填充 + 列表填充完成的最终报表")
    public ResponseEntity<byte[]> fillDownload() {
        return ExcelWebSupport.xlsx("销售订单-已填.xlsx", service.fillBytes());
    }

    @PostMapping("/fill-demo")
    @Operation(summary = "模板填充演示（JSON）", description = "说明模板占位符与填充结果")
    public ApiResponse<Map<String, Object>> fillDemo() {
        return ApiResponse.success(service.fillDemo());
    }

    @GetMapping("/explain")
    @Operation(summary = "模板填充速记（八股）", description = "withTemplate / FillWrapper / forceNewRow / 模板设计原则")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

package com.example.excel.mergehead;

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
 * 04. 复杂表头实验接口。
 */
@RestController
@RequestMapping("/api/mergehead")
@RequiredArgsConstructor
@Tag(name = "04. 复杂表头", description = "多级分组表头 / 纵向合并相同值 / 表头合并速记")
public class MergeHeadController {

    private final MergeHeadService service;

    @GetMapping("/download")
    @Operation(summary = "下载年度销售业绩表.xlsx", description = "2 级分组表头，区域按 3 行合并")
    public ResponseEntity<byte[]> download() {
        return ExcelWebSupport.xlsx("年度销售业绩表.xlsx", service.exportBytes());
    }

    @GetMapping("/export-demo")
    @Operation(summary = "导出演示（JSON）", description = "表头层级与合并说明")
    public ApiResponse<Map<String, Object>> exportDemo() {
        return ApiResponse.success(service.exportDemo());
    }

    @GetMapping("/explain")
    @Operation(summary = "复杂表头速记（八股）", description = "多级 value / 三种合并手段")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

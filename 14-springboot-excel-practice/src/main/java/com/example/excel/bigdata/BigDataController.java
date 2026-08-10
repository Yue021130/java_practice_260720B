package com.example.excel.bigdata;

import com.example.excel.common.ApiResponse;
import com.example.excel.support.ExcelWebSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 05. 大数据量导出实验接口。
 */
@RestController
@RequestMapping("/api/bigdata")
@RequiredArgsConstructor
@Tag(name = "05. 大数据量导出", description = "分页边查边写 / 内存对比 / 策略速记")
public class BigDataController {

    private final BigDataService service;

    @GetMapping("/download")
    @Operation(summary = "下载 N 行大数据.xlsx", description = "分页边查边写导出，内存恒定")
    public ResponseEntity<byte[]> download(@RequestParam(defaultValue = "50000") int rows) {
        return ExcelWebSupport.xlsx("员工大数据" + rows + "行.xlsx", service.exportBytes(rows));
    }

    @PostMapping("/export-demo")
    @Operation(summary = "导出演示（JSON）", description = "行数/页大小/耗时/文件大小，演示分页写")
    public ApiResponse<Map<String, Object>> exportDemo(@RequestParam(defaultValue = "50000") int rows) {
        return ApiResponse.success(service.exportDemo(rows));
    }

    @PostMapping("/compare")
    @Operation(summary = "内存对比（JSON）", description = "全量 List vs 分页边查边写的堆占用采样")
    public ApiResponse<Map<String, Object>> compare(@RequestParam(defaultValue = "50000") int rows) {
        return ApiResponse.success(service.compare(rows));
    }

    @GetMapping("/strategy")
    @Operation(summary = "大数据导出策略速记（八股）", description = "边查边写 / POI 内存对比 / 优化要点")
    public ApiResponse<Map<String, Object>> strategy() {
        return ApiResponse.success(service.strategy());
    }
}

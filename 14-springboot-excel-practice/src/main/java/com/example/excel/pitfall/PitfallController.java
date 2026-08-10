package com.example.excel.pitfall;

import com.example.excel.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 10. 常见坑与调优实验接口。
 */
@RestController
@RequestMapping("/api/pitfall")
@RequiredArgsConstructor
@Tag(name = "10. 常见坑与调优", description = "10 个高频坑 / EasyExcel vs POI / 表头不匹配现场 / 调优要点")
public class PitfallController {

    private final PitfallService service;

    @GetMapping("/list")
    @Operation(summary = "10 个高频坑清单", description = "每个坑：现象 → 原因 → 解法")
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/poi-vs-easyexcel")
    @Operation(summary = "EasyExcel vs POI 对比（八股）", description = "内存 / 代码量 / 格式 / 适用场景")
    public ApiResponse<Map<String, Object>> poiVsEasyexcel() {
        return ApiResponse.success(service.poiVsEasyexcel());
    }

    @GetMapping("/head-mismatch-demo")
    @Operation(summary = "表头不匹配现场演示", description = "列名差一个字，id 字段静默丢失——亲眼看看这个坑")
    public ApiResponse<Map<String, Object>> headMismatchDemo() {
        return ApiResponse.success(service.headMismatchDemo());
    }

    @GetMapping("/tuning")
    @Operation(summary = "调优要点（八股）", description = "读 / 写 / 线程池 / 限流 / 超大文件方案")
    public ApiResponse<Map<String, Object>> tuning() {
        return ApiResponse.success(service.tuning());
    }
}

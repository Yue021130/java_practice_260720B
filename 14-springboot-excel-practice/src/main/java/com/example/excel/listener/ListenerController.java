package com.example.excel.listener;

import com.example.excel.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 07. 监听器与增量读取实验接口。
 */
@RestController
@RequestMapping("/api/listener")
@RequiredArgsConstructor
@Tag(name = "07. 监听器与增量读取", description = "AnalysisEventListener 流式读 / 按批落库 / 回调机制速记")
public class ListenerController {

    private final ListenerService service;

    @PostMapping("/import-demo")
    @Operation(summary = "监听器导入演示（JSON）", description = "生成 N 行 → 流式逐行回调 → 每批批量落库，返回批次统计")
    public ApiResponse<Map<String, Object>> importDemo(@RequestParam(defaultValue = "250") int rows) {
        return ApiResponse.success(service.importDemo(rows));
    }

    @GetMapping("/explain")
    @Operation(summary = "监听器机制速记（八股）", description = "为何用监听器 / 回调方法 / 批量与断点续传")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

package com.example.comm.summary;

import com.example.comm.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 10. 选型总结与底层统一模型。
 */
@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Tag(name = "10. 选型总结", description = "需求场景选型表 / 底层统一模型（阻塞+等待队列+唤醒）")
public class SummaryController {

    private final SummaryService service;

    @GetMapping("/overview")
    @Operation(summary = "七大类总览", description = "线程间通信方式全景图（共享内存/等待通知/协作/同步工具/队列/异步/通道）")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(service.overview());
    }

    @GetMapping("/decision-table")
    @Operation(summary = "选型表", description = "需求场景 → 首选方案的速查表")
    public ApiResponse<Map<String, Object>> decisionTable() {
        return ApiResponse.success(service.decisionTable());
    }

    @GetMapping("/unified-model")
    @Operation(summary = "底层统一模型", description = "一切线程通信都是「阻塞 + 等待队列 + 唤醒」→ 落到 futex")
    public ApiResponse<Map<String, Object>> unifiedModel() {
        return ApiResponse.success(service.unifiedModel());
    }

    @GetMapping("/explain")
    @Operation(summary = "总结速记（八股）", description = "七大类一句话记忆 + 殊途同归")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

package com.example.juc.sync;

import com.example.juc.common.ApiResponse;
import com.example.juc.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 同步工具场景 REST 接口。
 *
 * 覆盖 CountDownLatch、CyclicBarrier、Semaphore、Exchanger。
 */
@Tag(name = "同步工具场景", description = "CountDownLatch / CyclicBarrier / Semaphore / Exchanger")
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncScenarioService syncScenarioService;

    @Operation(summary = "CountDownLatch 并行启动自检", description = "DB/Redis/MQ 并行检查，全部就绪才放行；另演示超时场景")
    @PostMapping("/latch-startup")
    public ApiResponse<ScenarioResult> latchStartup() {
        return ApiResponse.success(syncScenarioService.latchStartup());
    }

    @Operation(summary = "CyclicBarrier 分片报表汇总", description = "4 线程各算一季度财报，人齐后合并年报；可复用跑多轮")
    @PostMapping("/barrier-report")
    public ApiResponse<ScenarioResult> barrierReport(
            @Parameter(description = "运行轮数", example = "2") @RequestParam(defaultValue = "2") int rounds) {
        if (rounds < 1 || rounds > 3) {
            return ApiResponse.error(400, "rounds 必须在 1~3 之间");
        }
        return ApiResponse.success(syncScenarioService.barrierReport(rounds));
    }

    @Operation(summary = "Semaphore 接口限流", description = "模拟只允许 N 并发的下游通道，统计峰值并发与降级数量")
    @PostMapping("/semaphore-limit")
    public ApiResponse<ScenarioResult> semaphoreLimit(
            @Parameter(description = "请求数", example = "10") @RequestParam(defaultValue = "10") int requests,
            @Parameter(description = "许可数", example = "3") @RequestParam(defaultValue = "3") int permits) {
        if (requests < 1 || requests > 50 || permits < 1 || permits > 10) {
            return ApiResponse.error(400, "参数越界");
        }
        return ApiResponse.success(syncScenarioService.semaphoreLimit(requests, permits));
    }

    @Operation(summary = "Exchanger 双人对账", description = "两线程各核一半账单，在 exchange 点互换并交叉验证；演示单边超时")
    @PostMapping("/exchanger-reconcile")
    public ApiResponse<ScenarioResult> exchangerReconcile() {
        return ApiResponse.success(syncScenarioService.exchangerReconcile());
    }
}

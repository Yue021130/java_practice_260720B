package com.example.comm.sync;

import com.example.comm.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 06. JUC 同步工具（基于 AQS 封装的高层语义）。
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "06. JUC 同步工具", description = "CountDownLatch / CyclicBarrier / Semaphore / Exchanger / Phaser")
public class SyncController {

    private final SyncService service;

    @GetMapping("/latch-demo")
    @Operation(summary = "CountDownLatch 倒计时门闩", description = "主线程等 N 个 worker 完成（一次性）")
    public ApiResponse<Map<String, Object>> latchDemo(@RequestParam(defaultValue = "3") int workers) {
        return ApiResponse.success(service.latchDemo(workers));
    }

    @GetMapping("/barrier-demo")
    @Operation(summary = "CyclicBarrier 循环栅栏", description = "N 线程到齐才放行，可循环多轮（可复用）")
    public ApiResponse<Map<String, Object>> barrierDemo(@RequestParam(defaultValue = "3") int parties,
                                                        @RequestParam(defaultValue = "3") int rounds) {
        return ApiResponse.success(service.barrierDemo(parties, rounds));
    }

    @GetMapping("/semaphore-demo")
    @Operation(summary = "Semaphore 信号量限流", description = "同时最多 permits 个线程进入临界区")
    public ApiResponse<Map<String, Object>> semaphoreDemo(@RequestParam(defaultValue = "2") int permits,
                                                          @RequestParam(defaultValue = "8") int threads) {
        return ApiResponse.success(service.semaphoreDemo(permits, threads));
    }

    @GetMapping("/exchanger-demo")
    @Operation(summary = "Exchanger 数据交换点", description = "两个线程在此碰头，双向交换数据")
    public ApiResponse<Map<String, Object>> exchangerDemo() {
        return ApiResponse.success(service.exchangerDemo());
    }

    @GetMapping("/phaser-demo")
    @Operation(summary = "Phaser 阶段器", description = "多阶段同步 / 动态增减参与者（Latch+Barrier 合体）")
    public ApiResponse<Map<String, Object>> phaserDemo(@RequestParam(defaultValue = "3") int parties) {
        return ApiResponse.success(service.phaserDemo(parties));
    }

    @GetMapping("/explain")
    @Operation(summary = "同步工具速记（八股）", description = "四件套对比表 / 一次性 vs 循环 / 适用场景")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

package com.example.juc.base;

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
 * JMM 基础场景 REST 接口。
 *
 * 覆盖 volatile 可见性/原子性、DCL 双重检查单例、死锁检测。
 */
@Tag(name = "JMM 基础场景", description = "volatile / DCL / 死锁检测")
@RestController
@RequestMapping("/api/base")
@RequiredArgsConstructor
public class BaseController {

    private final BaseScenarioService baseScenarioService;

    @Operation(summary = "volatile 可见性", description = "普通变量 vs volatile 变量，worker 多久能看到 stop 信号")
    @PostMapping("/volatile-visibility")
    public ApiResponse<ScenarioResult> volatileVisibility() {
        return ApiResponse.success(baseScenarioService.volatileVisibility());
    }

    @Operation(summary = "volatile 不保证原子性", description = "多线程对 volatile int 做 i++，结果远小于预期")
    @PostMapping("/volatile-not-atomic")
    public ApiResponse<ScenarioResult> volatileNotAtomic(
            @Parameter(description = "线程数", example = "8") @RequestParam(defaultValue = "8") int threads,
            @Parameter(description = "每线程自增次数", example = "1000") @RequestParam(defaultValue = "1000") int times) {
        if (threads < 1 || threads > 32 || times < 100 || times > 10000) {
            return ApiResponse.error(400, "参数越界");
        }
        return ApiResponse.success(baseScenarioService.volatileNotAtomic(threads, times));
    }

    @Operation(summary = "DCL 双重检查单例", description = "验证正确 DCL（volatile）与静态内部类 Holder 的单例唯一性")
    @PostMapping("/dcl-singleton")
    public ApiResponse<ScenarioResult> dclSingleton() {
        return ApiResponse.success(baseScenarioService.dclSingleton());
    }

    @Operation(summary = "死锁制造与自动检测", description = "故意制造死锁，用 ThreadMXBean 自动检测并打印栈")
    @PostMapping("/deadlock-detect")
    public ApiResponse<ScenarioResult> deadlockDetect() {
        return ApiResponse.success(baseScenarioService.deadlockDetect());
    }
}

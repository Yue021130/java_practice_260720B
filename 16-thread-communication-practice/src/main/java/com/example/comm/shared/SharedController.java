package com.example.comm.shared;

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
 * 01. 基于共享内存：volatile 可见性 / 原子类 CAS。
 */
@RestController
@RequestMapping("/api/shared")
@RequiredArgsConstructor
@Tag(name = "01. 共享变量与原子类", description = "volatile 可见性 / AtomicXxx(CAS) 原子性")
public class SharedController {

    private final SharedService service;

    @GetMapping("/volatile-demo")
    @Operation(summary = "volatile 可见性演示", description = "主线程置标志，N 个工作线程轮询感知，统计感知延迟")
    public ApiResponse<Map<String, Object>> volatileDemo(@RequestParam(defaultValue = "4") int workers,
                                                         @RequestParam(defaultValue = "200") int flagDelayMs) {
        return ApiResponse.success(service.volatileDemo(workers, flagDelayMs));
    }

    @GetMapping("/atomic-demo")
    @Operation(summary = "原子类 CAS 演示", description = "N 线程各自累加：普通 int++ 丢失 vs AtomicInteger 不丢")
    public ApiResponse<Map<String, Object>> atomicDemo(@RequestParam(defaultValue = "4") int threads,
                                                       @RequestParam(defaultValue = "1000") int increments) {
        return ApiResponse.success(service.atomicDemo(threads, increments));
    }

    @GetMapping("/explain")
    @Operation(summary = "volatile / CAS 速记（八股）", description = "三性 / 原子性边界 / ABA / 适用场景")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

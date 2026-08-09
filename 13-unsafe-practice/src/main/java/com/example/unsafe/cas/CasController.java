package com.example.unsafe.cas;

import com.example.unsafe.common.ApiResponse;
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
 * 04. CAS 原子操作实验接口。
 */
@RestController
@RequestMapping("/api/cas")
@RequiredArgsConstructor
@Tag(name = "04. CAS 原子操作", description = "自旋计数器 / 三种自增性能对比 / ABA 问题 / 原理八股")
public class CasController {

    private final CasService service;

    @PostMapping("/spin")
    @Operation(summary = "自旋 CAS 计数器", description = "getIntVolatile + compareAndSwapInt 循环自增，统计 CAS 尝试次数")
    public ApiResponse<Map<String, Object>> spin(@RequestParam(defaultValue = "100000") int times) {
        return ApiResponse.success(service.spin(times));
    }

    @PostMapping("/benchmark")
    @Operation(summary = "三种自增性能对比", description = "synchronized / AtomicInteger / Unsafe CAS 并发自增耗时对比")
    public ApiResponse<Map<String, Object>> benchmark(
            @RequestParam(defaultValue = "4") int threads,
            @RequestParam(defaultValue = "200000") int times) {
        return ApiResponse.success(service.benchmark(threads, times));
    }

    @GetMapping("/aba")
    @Operation(summary = "ABA 问题现场复现", description = "线程 B 折腾 100→200→100，比较无版本号与带版本号两种 CAS 的结局")
    public ApiResponse<Map<String, Object>> aba() {
        return ApiResponse.success(service.aba());
    }

    @GetMapping("/explain")
    @Operation(summary = "CAS 原理八股速记", description = "是什么 / 与锁的区别 / 三大问题 / JUC 谁在用")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

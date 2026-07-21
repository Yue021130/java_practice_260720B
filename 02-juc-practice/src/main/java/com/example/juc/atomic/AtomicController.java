package com.example.juc.atomic;

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
 * 原子类场景 REST 接口。
 *
 * 覆盖 CAS 原理、AtomicLong、LongAdder 分段热点、ABA 问题、无锁状态机。
 */
@Tag(name = "原子类场景", description = "CAS / AtomicLong / LongAdder / ABA / 无锁状态机")
@RestController
@RequestMapping("/api/atomic")
@RequiredArgsConstructor
public class AtomicController {

    private final AtomicScenarioService atomicScenarioService;

    @Operation(summary = "库存扣减三路对比", description = "普通 int / synchronized / AtomicLong 同场对比正确性与耗时")
    @PostMapping("/stock-compare")
    public ApiResponse<ScenarioResult> stockCompare(
            @Parameter(description = "线程数", example = "8") @RequestParam(defaultValue = "8") int threads,
            @Parameter(description = "每线程扣减次数", example = "10000") @RequestParam(defaultValue = "10000") int times) {
        if (threads < 1 || threads > 32 || times < 1000 || times > 100000) {
            return ApiResponse.error(400, "参数越界");
        }
        return ApiResponse.success(atomicScenarioService.stockCompare(threads, times));
    }

    @Operation(summary = "LongAdder vs AtomicLong 压测", description = "高并发计数下两者的吞吐与准确性对比")
    @PostMapping("/longadder-vs-atomic")
    public ApiResponse<ScenarioResult> longadderVsAtomic(
            @Parameter(description = "线程数", example = "32") @RequestParam(defaultValue = "32") int threads,
            @Parameter(description = "每线程累加次数", example = "100000") @RequestParam(defaultValue = "100000") int times) {
        if (threads < 1 || threads > 64 || times < 10000 || times > 1000000) {
            return ApiResponse.error(400, "参数越界");
        }
        return ApiResponse.success(atomicScenarioService.longadderVsAtomic(threads, times));
    }

    @Operation(summary = "ABA 问题与修复", description = "AtomicReference 复现 ABA，AtomicStampedReference 用版本号识破")
    @PostMapping("/aba")
    public ApiResponse<ScenarioResult> aba() {
        return ApiResponse.success(atomicScenarioService.aba());
    }

    @Operation(summary = "无锁订单状态机", description = "用 CAS 保证订单状态迁移只发生一次，防止重复支付")
    @PostMapping("/order-state")
    public ApiResponse<ScenarioResult> orderState() {
        return ApiResponse.success(atomicScenarioService.orderState());
    }
}

package com.example.juc.locks;

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
 * 锁场景 REST 接口。
 *
 * 覆盖 ReentrantLock（公平/非公平/可中断/tryLock）、ReentrantReadWriteLock、
 * StampedLock、Condition、LockSupport。每个场景都对应一个现实业务模型和面试考点。
 */
@Tag(name = "锁场景", description = "ReentrantLock / 读写锁 / StampedLock / Condition / LockSupport")
@RestController
@RequestMapping("/api/locks")
@RequiredArgsConstructor
public class LockController {

    private final LockScenarioService lockScenarioService;

    @Operation(summary = "转账死锁与解决", description = "演示交叉加锁造成的死锁，以及 tryLock 超时 + 按序加锁的破解方案")
    @PostMapping("/transfer-deadlock")
    public ApiResponse<ScenarioResult> transferDeadlock(
            @Parameter(description = "运行模式：deadlock 演示死锁征兆，trylock 演示按序超时重试")
            @RequestParam(defaultValue = "trylock") String mode) {
        if (!"deadlock".equals(mode) && !"trylock".equals(mode)) {
            return ApiResponse.error(400, "mode 只能是 deadlock 或 trylock");
        }
        return ApiResponse.success(lockScenarioService.transferDeadlock(mode));
    }

    @Operation(summary = "公平锁 vs 非公平锁", description = "同一线程数抢锁多次，对比公平锁排队顺序与非公平锁吞吐量")
    @PostMapping("/fair-vs-nonfair")
    public ApiResponse<ScenarioResult> fairVsNonfair(
            @Parameter(description = "线程数", example = "8")
            @RequestParam(defaultValue = "8") int threads) {
        if (threads < 1 || threads > 32) {
            return ApiResponse.error(400, "threads 必须在 1~32 之间");
        }
        return ApiResponse.success(lockScenarioService.fairVsNonfair(threads));
    }

    @Operation(summary = "可中断锁 lockInterruptibly", description = "演示 Lock 的 lockInterruptibly 在等锁期间可被中断，synchronized 做不到")
    @PostMapping("/interruptible")
    public ApiResponse<ScenarioResult> interruptible() {
        return ApiResponse.success(lockScenarioService.interruptible());
    }

    @Operation(summary = "读写锁商品缓存", description = "读多写少场景下对比 ReentrantReadWriteLock 与独占 ReentrantLock 的并发读性能")
    @PostMapping("/readwrite-cache")
    public ApiResponse<ScenarioResult> readwriteCache(
            @Parameter(description = "读线程数", example = "10")
            @RequestParam(defaultValue = "10") int readers,
            @Parameter(description = "写线程数", example = "2")
            @RequestParam(defaultValue = "2") int writers) {
        if (readers < 1 || readers > 32 || writers < 1 || writers > 8) {
            return ApiResponse.error(400, "readers 1~32，writers 1~8");
        }
        return ApiResponse.success(lockScenarioService.readwriteCache(readers, writers));
    }

    @Operation(summary = "StampedLock 乐观读", description = "物流坐标读多写少：乐观读无锁，validate 失败再升级为悲观读锁")
    @PostMapping("/stamped-optimistic")
    public ApiResponse<ScenarioResult> stampedOptimistic() {
        return ApiResponse.success(lockScenarioService.stampedOptimistic());
    }

    @Operation(summary = "Condition 手写生产者消费者", description = "不用 BlockingQueue，用 ReentrantLock + 两个 Condition 手写有界缓冲区")
    @PostMapping("/condition-buffer")
    public ApiResponse<ScenarioResult> conditionBuffer(
            @Parameter(description = "生产者数", example = "3")
            @RequestParam(defaultValue = "3") int producers,
            @Parameter(description = "消费者数", example = "3")
            @RequestParam(defaultValue = "3") int consumers,
            @Parameter(description = "每个生产者生产的数量", example = "10")
            @RequestParam(defaultValue = "10") int items) {
        if (producers < 1 || producers > 16 || consumers < 1 || consumers > 16 || items < 1 || items > 50) {
            return ApiResponse.error(400, "producers/consumers 1~16，items 1~50");
        }
        return ApiResponse.success(lockScenarioService.conditionBuffer(producers, consumers, items));
    }

    @Operation(summary = "LockSupport 演示", description = "演示许可不累积、先 unpark 后 park 不阻塞、park 响应中断不抛异常")
    @PostMapping("/locksupport")
    public ApiResponse<ScenarioResult> lockSupport() {
        return ApiResponse.success(lockScenarioService.lockSupport());
    }
}

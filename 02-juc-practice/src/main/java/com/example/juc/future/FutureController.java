package com.example.juc.future;

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
 * 异步编排场景 REST 接口。
 *
 * 覆盖 CompletableFuture、老式 Future、ForkJoin、ScheduledThreadPoolExecutor。
 */
@Tag(name = "异步编排场景", description = "CompletableFuture / Future / ForkJoin / 定时调度")
@RestController
@RequestMapping("/api/future")
@RequiredArgsConstructor
public class FutureController {

    private final FutureScenarioService futureScenarioService;

    @Operation(summary = "CompletableFuture 电商详情页聚合", description = "并行编排 / thenCompose / applyToEither / 超时兜底 / 异常降级")
    @PostMapping("/cf-detail")
    public ApiResponse<ScenarioResult> cfDetail() {
        return ApiResponse.success(futureScenarioService.cfDetail());
    }

    @Operation(summary = "老式 Future 对比", description = "同样的聚合用 Future.get 串行写，反衬 CompletableFuture")
    @PostMapping("/old-future")
    public ApiResponse<ScenarioResult> oldFuture() {
        return ApiResponse.success(futureScenarioService.oldFuture());
    }

    @Operation(summary = "ForkJoin 分治求和", description = "递归拆分任务 vs 单线程循环求和")
    @PostMapping("/forkjoin-sum")
    public ApiResponse<ScenarioResult> forkjoinSum(
            @Parameter(description = "数组规模", example = "100000000") @RequestParam(defaultValue = "100000000") int size) {
        if (size < 1_000_000 || size > 200_000_000) {
            return ApiResponse.error(400, "size 必须在 1000000~200000000 之间");
        }
        return ApiResponse.success(futureScenarioService.forkjoinSum(size));
    }

    @Operation(summary = "定时任务两种模式对比", description = "scheduleAtFixedRate 追赶 vs scheduleWithFixedDelay 顺延，并演示异常后静默停止")
    @PostMapping("/scheduled-compare")
    public ApiResponse<ScenarioResult> scheduledCompare() {
        return ApiResponse.success(futureScenarioService.scheduledCompare());
    }
}

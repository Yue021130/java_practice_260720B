package com.example.juc.threadlocal;

import com.example.juc.common.ApiResponse;
import com.example.juc.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ThreadLocal 场景 REST 接口。
 *
 * 覆盖 traceId 全链路传递、内存泄漏与串号、线程池下上下文丢失。
 */
@Tag(name = "ThreadLocal 场景", description = "ThreadLocal 原理与坑")
@RestController
@RequestMapping("/api/threadlocal")
@RequiredArgsConstructor
public class ThreadLocalController {

    private final ThreadLocalScenarioService threadLocalScenarioService;

    @Operation(summary = "traceId 全链路传递", description = "模拟拦截器生成 traceId 后各层读取，finally remove")
    @PostMapping("/trace-chain")
    public ApiResponse<ScenarioResult> traceChain() {
        return ApiResponse.success(threadLocalScenarioService.traceChain());
    }

    @Operation(summary = "泄漏与串号演示", description = "线程池复用线程且不 remove，下一个请求读到上一个请求的上下文")
    @PostMapping("/leak-demo")
    public ApiResponse<ScenarioResult> leakDemo() {
        return ApiResponse.success(threadLocalScenarioService.leakDemo());
    }

    @Operation(summary = "线程池下上下文丢失", description = "演示父线程上下文传不进异步任务，以及手动传递、装饰器两种补救")
    @PostMapping("/pool-context")
    public ApiResponse<ScenarioResult> poolContext() {
        return ApiResponse.success(threadLocalScenarioService.poolContext());
    }
}

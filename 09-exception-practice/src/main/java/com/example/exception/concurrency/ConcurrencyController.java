package com.example.exception.concurrency;

import com.example.exception.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 并发中的异常实验接口。
 */
@RestController
@RequestMapping("/api/concurrency")
@RequiredArgsConstructor
@Tag(name = "06. 并发中的异常", description = "线程异常传播、Future.get、CompletableFuture、@Async、线程池吞异常")
public class ConcurrencyController {

    private final ConcurrencyScenarioService service;
    private final AsyncTaskService asyncTaskService;

    @PostMapping("/thread-uncaught")
    @Operation(summary = "子线程异常不抛主线程", description = "默认情况下子线程异常不会传播到主线程")
    public ApiResponse<Map<String, Object>> threadUncaught() throws InterruptedException {
        return ApiResponse.success(service.threadUncaught());
    }

    @PostMapping("/uncaught-handler")
    @Operation(summary = "UncaughtExceptionHandler", description = "统一捕获子线程未处理异常")
    public ApiResponse<Map<String, Object>> uncaughtHandler() throws InterruptedException {
        return ApiResponse.success(service.uncaughtHandler());
    }

    @PostMapping("/future-get")
    @Operation(summary = "Future.get 异常包装", description = "任务异常被包装为 ExecutionException")
    public ApiResponse<Map<String, Object>> futureGet() {
        return ApiResponse.success(service.futureGet());
    }

    @PostMapping("/completable-exception")
    @Operation(summary = "CompletableFuture 异常处理", description = "exceptionally / handle / whenComplete 区别")
    public ApiResponse<Map<String, Object>> completableException() {
        return ApiResponse.success(service.completableException());
    }

    @PostMapping("/async-exception")
    @Operation(summary = "@Async 异常", description = "通过 AsyncUncaughtExceptionHandler 捕获（看控制台）")
    public ApiResponse<Map<String, Object>> asyncException() {
        asyncTaskService.asyncWithException();
        return ApiResponse.success(service.asyncException());
    }

    @PostMapping("/pool-swallow")
    @Operation(summary = "线程池 submit 吞异常", description = "submit vs execute 的异常处理差异")
    public ApiResponse<Map<String, Object>> poolSwallow() throws InterruptedException {
        return ApiResponse.success(service.poolSwallow());
    }
}

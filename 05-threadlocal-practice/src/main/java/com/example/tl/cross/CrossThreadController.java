package com.example.tl.cross;

import com.example.tl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cross")
@RequiredArgsConstructor
@Tag(name = "跨线程", description = "InheritableThreadLocal / 线程池污染 / TTL 透传")
public class CrossThreadController {

    private final CrossThreadService crossThreadService;

    @PostMapping("/inheritable")
    @Operation(summary = "InheritableThreadLocal", description = "new Thread 子线程继承，线程池不继承")
    public ApiResponse<Map<String, Object>> inheritable() throws Exception {
        return ApiResponse.success(crossThreadService.inheritableDemo());
    }

    @PostMapping("/pool-hazard")
    @Operation(summary = "线程池串号 / 污染", description = "未 remove 导致后续任务读到残留值")
    public ApiResponse<Map<String, Object>> poolHazard() throws Exception {
        return ApiResponse.success(crossThreadService.poolHazardDemo());
    }

    @PostMapping("/pool-remove")
    @Operation(summary = "线程池正确使用", description = "任务 finally 中 remove，避免串号")
    public ApiResponse<Map<String, Object>> poolRemove() throws Exception {
        return ApiResponse.success(crossThreadService.poolRemoveDemo());
    }

    @PostMapping("/async-context")
    @Operation(summary = "CompletableFuture 上下文丢失", description = "默认 ForkJoinPool 不继承 ThreadLocal")
    public ApiResponse<Map<String, Object>> asyncContext() throws Exception {
        return ApiResponse.success(crossThreadService.asyncContextDemo());
    }

    @PostMapping("/ttl-propagation")
    @Operation(summary = "TTL 线程池透传", description = "Alibaba TransmittableThreadLocal 实现线程池上下文透传")
    public ApiResponse<Map<String, Object>> ttlPropagation() throws Exception {
        return ApiResponse.success(crossThreadService.ttlPropagationDemo());
    }
}

package com.example.tl.webctx;

import com.example.tl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
@Tag(name = "Web 上下文", description = "Filter + ThreadLocal / MDC traceId / SimpleDateFormat 线程安全")
public class WebContextController {

    private final WebContextService webContextService;

    @PostMapping("/user-context")
    @Operation(summary = "Filter + ThreadLocal 传递用户", description = "从 Header 解析用户并放入 ThreadLocal，Service 层直接读取")
    public ApiResponse<Map<String, Object>> userContext() {
        return ApiResponse.success(webContextService.userContextDemo());
    }

    @PostMapping("/mdc-trace")
    @Operation(summary = "MDC 全链路 traceId", description = "通过 MDC 在日志中输出 traceId")
    public ApiResponse<Map<String, Object>> mdcTrace() {
        return ApiResponse.success(webContextService.mdcTraceDemo());
    }

    @PostMapping("/dateformat-safe")
    @Operation(summary = "SimpleDateFormat 线程安全", description = "共享 SimpleDateFormat 并发异常 vs ThreadLocal 解决")
    public ApiResponse<Map<String, Object>> dateFormatSafe() throws InterruptedException {
        return ApiResponse.success(webContextService.dateFormatSafeDemo());
    }
}

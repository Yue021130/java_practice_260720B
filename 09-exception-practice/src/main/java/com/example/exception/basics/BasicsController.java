package com.example.exception.basics;

import com.example.exception.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 异常基础语法实验接口。
 */
@RestController
@RequestMapping("/api/basics")
@RequiredArgsConstructor
@Tag(name = "02. 异常基础语法", description = "try-catch-finally、try-with-resources、异常链、脱敏")
public class BasicsController {

    private final BasicsScenarioService service;

    @PostMapping("/execution-order")
    @Operation(summary = "try-catch-finally 执行顺序", description = "normal / catch / uncaught / return 四种场景")
    public ApiResponse<Map<String, Object>> executionOrder(
            @Parameter(description = "场景：normal, catch, uncaught, return")
            @RequestParam(defaultValue = "normal") String scenario) {
        return ApiResponse.success(service.executionOrder(scenario));
    }

    @PostMapping("/finally-override")
    @Operation(summary = "finally 覆盖返回值或异常", description = "演示 finally 中 return/throw 对 catch 中 throw 的覆盖")
    public ApiResponse<Map<String, Object>> finallyOverride(
            @Parameter(description = "true: finally return; false: finally throw")
            @RequestParam(defaultValue = "true") boolean withReturn) {
        return ApiResponse.success(service.finallyOverride(withReturn));
    }

    @PostMapping("/try-with-resources")
    @Operation(summary = "try-with-resources", description = "AutoCloseable 自动关闭与 Suppressed 异常")
    public ApiResponse<Map<String, Object>> tryWithResources(
            @Parameter(description = "是否让业务方法抛异常")
            @RequestParam(defaultValue = "false") boolean businessFail,
            @Parameter(description = "是否让 close 抛异常")
            @RequestParam(defaultValue = "false") boolean closeFail) throws Exception {
        return ApiResponse.success(service.tryWithResources(businessFail, closeFail));
    }

    @PostMapping("/exception-chain")
    @Operation(summary = "异常链 cause", description = "低层异常转业务异常时保留 cause")
    public ApiResponse<Map<String, Object>> exceptionChain() {
        return ApiResponse.success(service.exceptionChain());
    }

    @PostMapping("/mask-sensitive")
    @Operation(summary = "异常信息脱敏", description = "内部日志记录完整异常，对外返回脱敏信息")
    public ApiResponse<Map<String, Object>> maskSensitive() {
        return ApiResponse.success(service.maskSensitive());
    }

    @GetMapping("/finally-not-execute")
    @Operation(summary = "finally 不执行的极端情况", description = "System.exit / JVM 崩溃 / 线程被杀等")
    public ApiResponse<Map<String, Object>> finallyNotExecute() {
        return ApiResponse.success(service.finallyNotExecute());
    }
}

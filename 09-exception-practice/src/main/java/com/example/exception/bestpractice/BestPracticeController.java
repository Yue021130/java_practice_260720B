package com.example.exception.bestpractice;

import com.example.exception.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 异常最佳实践与反模式实验接口。
 */
@RestController
@RequestMapping("/api/bestpractice")
@RequiredArgsConstructor
@Tag(name = "07. 最佳实践与反模式", description = "吞异常、流程控制、fail-fast、日志规范、事务回滚")
public class BestPracticeController {

    private final BestPracticeScenarioService service;

    @PostMapping("/swallow")
    @Operation(summary = "不要吞异常", description = "空 catch 会让问题无法定位")
    public ApiResponse<Map<String, Object>> swallow() {
        return ApiResponse.success(service.swallowException());
    }

    @PostMapping("/flow-control")
    @Operation(summary = "不要用异常做流程控制", description = "异常创建开销大，应用 if/for/return 替代")
    public ApiResponse<Map<String, Object>> flowControl() {
        return ApiResponse.success(service.flowControl());
    }

    @PostMapping("/fail-fast")
    @Operation(summary = "早失败 fail-fast", description = "入参校验前置，避免错误扩散")
    public ApiResponse<Map<String, Object>> failFast() {
        return ApiResponse.success(service.failFast());
    }

    @PostMapping("/logging")
    @Operation(summary = "异常日志规范", description = "记录上下文 + 堆栈，避免重复打印")
    public ApiResponse<Map<String, Object>> logging() {
        return ApiResponse.success(service.logging());
    }

    @PostMapping("/transaction")
    @Operation(summary = "事务与异常回滚", description = "Spring 默认回滚规则与 rollbackFor 配置")
    public ApiResponse<Map<String, Object>> transaction() {
        return ApiResponse.success(service.transaction());
    }

    @PostMapping("/translate-or-pass")
    @Operation(summary = "异常转换 vs 透传", description = "跨层转换保留 cause，同层避免无意义 catch")
    public ApiResponse<Map<String, Object>> translateOrPass() {
        return ApiResponse.success(service.translateOrPass());
    }
}

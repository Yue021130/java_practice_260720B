package com.example.mail.retry;

import com.example.mail.common.ApiResponse;
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
 * 07. 失败重试邮件实验接口。
 */
@RestController
@RequestMapping("/api/retry")
@RequiredArgsConstructor
@Tag(name = "07. 失败重试", description = "指数退避重试、重试策略与告警")
public class RetryController {

    private final RetryMailService service;

    @PostMapping("/send")
    @Operation(summary = "带重试的发送", description = "failTimes 模拟前 N 次失败，观察退避后最终成功")
    public ApiResponse<Map<String, Object>> send(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【重试】重要通知") String subject,
            @RequestParam(defaultValue = "这条通知经过了失败重试最终送达。") String content,
            @RequestParam(defaultValue = "2") int failTimes,
            @RequestParam(defaultValue = "3") int maxRetries,
            @RequestParam(defaultValue = "exponential") String backoff) {
        return ApiResponse.success(service.sendWithRetry(to, subject, content, failTimes, maxRetries, backoff));
    }

    @GetMapping("/strategy")
    @Operation(summary = "重试策略说明", description = "固定 / 指数退避 / 抖动，以及重试纪律")
    public ApiResponse<Map<String, Object>> strategy() {
        return ApiResponse.success(service.strategy());
    }
}

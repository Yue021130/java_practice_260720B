package com.example.mail.async;

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
 * 06. 异步发送邮件实验接口。
 */
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Tag(name = "06. 异步发送", description = "@Async 后台发送、任务状态查询、线程池指标")
public class AsyncController {

    private final AsyncMailService service;

    @PostMapping("/send")
    @Operation(summary = "异步发送邮件", description = "接口立即返回 taskId，真正发送在 mailExecutor 线程池中执行")
    public ApiResponse<Map<String, Object>> send(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【异步】后台发送测试") String subject,
            @RequestParam(defaultValue = "这封邮件由线程池异步发送，请求已立即返回。") String content) {
        return ApiResponse.success(service.sendAsync(to, subject, content));
    }

    @GetMapping("/status")
    @Operation(summary = "查询异步任务状态", description = "PENDING → RUNNING → SENT / FAILED")
    public ApiResponse<Map<String, Object>> status(@RequestParam String taskId) {
        return ApiResponse.success(service.status(taskId));
    }

    @GetMapping("/pool")
    @Operation(summary = "异步线程池指标", description = "查看 mailExecutor 活跃线程 / 队列 / 完成任务数")
    public ApiResponse<Map<String, Object>> pool() {
        return ApiResponse.success(service.poolInfo());
    }
}

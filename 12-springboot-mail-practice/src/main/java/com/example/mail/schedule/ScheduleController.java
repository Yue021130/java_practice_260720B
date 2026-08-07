package com.example.mail.schedule;

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
 * 08. 定时 / 批量发送邮件实验接口。
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Tag(name = "08. 定时与批量", description = "批量发送、延迟任务、@Scheduled 心跳")
public class ScheduleController {

    private final ScheduleMailService service;

    @PostMapping("/batch")
    @Operation(summary = "批量发送", description = "循环发送 count 封邮件并统计成败")
    public ApiResponse<Map<String, Object>> batch(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【批量】报表") String subjectPrefix,
            @RequestParam(defaultValue = "5") int count) {
        return ApiResponse.success(service.batchSend(to, subjectPrefix, count));
    }

    @PostMapping("/register")
    @Operation(summary = "注册延迟发送任务", description = "delaySeconds 秒后在 mailScheduler 线程执行发送")
    public ApiResponse<Map<String, Object>> register(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【延迟】稍后送达") String subject,
            @RequestParam(defaultValue = "这条邮件将在指定延迟后自动发送。") String content,
            @RequestParam(defaultValue = "3") int delaySeconds) {
        return ApiResponse.success(service.registerDelayed(to, subject, content, delaySeconds));
    }

    @GetMapping("/list")
    @Operation(summary = "延迟任务列表", description = "查看已登记的延迟发送任务及状态")
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(service.listJobs());
    }

    @GetMapping("/heartbeat")
    @Operation(summary = "@Scheduled 心跳说明", description = "周期任务开关与真实用法")
    public ApiResponse<Map<String, Object>> heartbeat() {
        return ApiResponse.success(service.heartbeatInfo());
    }
}

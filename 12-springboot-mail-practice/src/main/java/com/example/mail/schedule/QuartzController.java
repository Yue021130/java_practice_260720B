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
 * Quartz 定时任务实验接口（挂在 08 定时与批量模块下）。
 */
@RestController
@RequestMapping("/api/schedule/quartz")
@RequiredArgsConstructor
@Tag(name = "08. Quartz 定时任务", description = "Cron 动态注册 / 列表 / 暂停 / 恢复 / 删除 / 概念说明")
public class QuartzController {

    private final QuartzMailService service;

    @PostMapping("/register")
    @Operation(summary = "注册 Cron 定时任务", description = "用 Cron 表达式注册一个定时发邮件任务（建议 simulate 模式体验）")
    public ApiResponse<Map<String, Object>> register(
            @RequestParam(defaultValue = "report-job") String jobName,
            @RequestParam(defaultValue = "0/30 * * * * ?") String cron,
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【Quartz】定时报表") String subject,
            @RequestParam(defaultValue = "这是 Quartz 定时任务自动发送的邮件。") String content) {
        return ApiResponse.success(service.registerCronJob(jobName, cron, to, subject, content));
    }

    @GetMapping("/list")
    @Operation(summary = "任务列表", description = "所有 Quartz 任务的触发器、状态与下次触发时间")
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(service.listJobs());
    }

    @PostMapping("/pause")
    @Operation(summary = "暂停任务", description = "不再触发但保留定义")
    public ApiResponse<Map<String, Object>> pause(@RequestParam String jobName) {
        return ApiResponse.success(service.pause(jobName));
    }

    @PostMapping("/resume")
    @Operation(summary = "恢复任务", description = "恢复被暂停的任务")
    public ApiResponse<Map<String, Object>> resume(@RequestParam String jobName) {
        return ApiResponse.success(service.resume(jobName));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除任务", description = "删除任务及其触发器")
    public ApiResponse<Map<String, Object>> delete(@RequestParam String jobName) {
        return ApiResponse.success(service.delete(jobName));
    }

    @GetMapping("/explain")
    @Operation(summary = "Quartz 概念速记", description = "Job / JobDetail / Trigger / Scheduler / Cron 示例 / 与 @Scheduled 对比")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

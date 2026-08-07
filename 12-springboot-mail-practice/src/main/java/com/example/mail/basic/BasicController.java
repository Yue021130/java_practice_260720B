package com.example.mail.basic;

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
 * 01. 基础邮件实验接口。
 */
@RestController
@RequestMapping("/api/basic")
@RequiredArgsConstructor
@Tag(name = "01. 基础邮件", description = "纯文本邮件 / 多收件人 / 抄送密送 / 最近发送记录")
public class BasicController {

    private final BasicMailService service;

    @PostMapping("/text")
    @Operation(summary = "发送纯文本邮件", description = "最简单的 MimeMessageHelper.setText(content)")
    public ApiResponse<Map<String, Object>> sendText(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【基础】欢迎使用邮件服务") String subject,
            @RequestParam(defaultValue = "这是一封最简单的纯文本邮件。\nHello from Spring Boot Mail!") String content) {
        return ApiResponse.success(service.sendText(to, subject, content));
    }

    @PostMapping("/multiple")
    @Operation(summary = "多收件人 + 抄送 + 密送", description = "setTo / setCc / setBcc，逗号分隔多个地址")
    public ApiResponse<Map<String, Object>> sendMultiple(
            @RequestParam(defaultValue = "a@example.com,b@example.com") String to,
            @RequestParam(defaultValue = "manager@example.com") String cc,
            @RequestParam(defaultValue = "hr@example.com") String bcc,
            @RequestParam(defaultValue = "【抄送】月度报告") String subject,
            @RequestParam(defaultValue = "本月业绩简报见正文附件。") String content) {
        return ApiResponse.success(service.sendMultiple(to, cc, bcc, subject, content));
    }

    @GetMapping("/recent")
    @Operation(summary = "最近发送记录", description = "内存中最近 50 封邮件的主题/收件人/大小/耗时")
    public ApiResponse<Map<String, Object>> recent() {
        return ApiResponse.success(service.recent());
    }

    @GetMapping("/mode")
    @Operation(summary = "当前发送模式", description = "simulate 模拟发送 / real 真实发送，及 SMTP 配置概览")
    public ApiResponse<Map<String, Object>> mode() {
        return ApiResponse.success(service.mode());
    }
}

package com.example.mail.header;

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
 * 09. 邮件头与编码实验接口。
 */
@RestController
@RequestMapping("/api/header")
@RequiredArgsConstructor
@Tag(name = "09. 邮件头与编码", description = "自定义邮件头、RFC 2047 主题编码")
public class HeaderController {

    private final HeaderMailService service;

    @PostMapping("/send")
    @Operation(summary = "发送带自定义头的邮件", description = "X-Priority / Reply-To / X-Mailer，中文主题自动编码")
    public ApiResponse<Map<String, Object>> send(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【紧急】服务器磁盘告警！") String subject,
            @RequestParam(defaultValue = "ops@example.com") String replyTo,
            @RequestParam(defaultValue = "1") String priority) {
        return ApiResponse.success(service.send(to, subject, replyTo, priority));
    }

    @PostMapping("/encoding")
    @Operation(summary = "RFC 2047 主题编码演示", description = "对比 JavaMail 自动编码与手动 Base64 编码结果")
    public ApiResponse<Map<String, Object>> encoding(
            @RequestParam(defaultValue = "中文主题测试：编码演示") String subject) {
        return ApiResponse.success(service.encoding(subject));
    }

    @GetMapping("/rules")
    @Operation(summary = "常用邮件头速查", description = "各邮件头含义与反垃圾配置提示")
    public ApiResponse<Map<String, Object>> rules() {
        return ApiResponse.success(service.rules());
    }
}

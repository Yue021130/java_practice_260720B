package com.example.mail.html;

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
 * 02. 富文本 HTML 邮件实验接口。
 */
@RestController
@RequestMapping("/api/html")
@RequiredArgsConstructor
@Tag(name = "02. 富文本 HTML 邮件", description = "HTML 内容、表格布局、链接按钮")
public class HtmlController {

    private final HtmlMailService service;

    @PostMapping("/send")
    @Operation(summary = "发送 HTML 邮件", description = "setText(html, true) 标记为 HTML 富文本")
    public ApiResponse<Map<String, Object>> send(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【HTML】支付成功通知") String subject,
            @RequestParam(defaultValue = "张三") String username,
            @RequestParam(defaultValue = "1288.50") double amount) {
        return ApiResponse.success(service.sendHtml(to, subject, username, amount));
    }

    @GetMapping("/example")
    @Operation(summary = "示例 HTML", description = "返回本模块使用的 HTML 源码，便于对比学习")
    public ApiResponse<Map<String, Object>> example() {
        return ApiResponse.success(service.example());
    }
}

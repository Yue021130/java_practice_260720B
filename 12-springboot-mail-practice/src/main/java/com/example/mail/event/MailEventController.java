package com.example.mail.event;

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
 * 11. 事件监听实验接口。
 */
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
@Tag(name = "11. 事件监听 @EventListener", description = "邮件发送事件、统计监听、异步通知监听、监听器扫描")
public class MailEventController {

    private final MailEventService service;

    @PostMapping("/send")
    @Operation(summary = "发送邮件触发事件链路", description = "发送成功后发布 MailSentEvent，多个监听器响应")
    public ApiResponse<Map<String, Object>> send(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【事件】触发监听链路") String subject,
            @RequestParam(defaultValue = "这封邮件会触发 @EventListener 事件监听器。") String content) {
        return ApiResponse.success(service.send(to, subject, content));
    }

    @PostMapping("/publish-demo")
    @Operation(summary = "手动发布事件", description = "发布成功/失败事件，观察各监听器响应（看控制台）")
    public ApiResponse<Map<String, Object>> publishDemo() {
        return ApiResponse.success(service.publishDemo());
    }

    @GetMapping("/stats")
    @Operation(summary = "事件统计", description = "监听器聚合的发送成功/失败统计（按场景标签）")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(service.stats());
    }

    @GetMapping("/listeners")
    @Operation(summary = "监听器清单", description = "扫描容器内所有 @EventListener 方法")
    public ApiResponse<Map<String, Object>> listeners() {
        return ApiResponse.success(service.listeners());
    }

    @GetMapping("/explain")
    @Operation(summary = "事件监听知识点", description = "@EventListener / condition / @Async / @TransactionalEventListener")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

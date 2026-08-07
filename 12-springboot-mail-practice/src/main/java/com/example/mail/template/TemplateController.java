package com.example.mail.template;

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
 * 05. Thymeleaf 模板邮件实验接口。
 */
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
@Tag(name = "05. Thymeleaf 模板邮件", description = "welcome.html / order.html 模板渲染、变量说明")
public class TemplateController {

    private final TemplateMailService service;

    @PostMapping("/welcome")
    @Operation(summary = "发送欢迎模板邮件", description = "渲染 templates/mail/welcome.html")
    public ApiResponse<Map<String, Object>> sendWelcome(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "张三") String username,
            @RequestParam(defaultValue = "Java高级知识") String platform) {
        return ApiResponse.success(service.sendWelcome(to, username, platform));
    }

    @PostMapping("/order")
    @Operation(summary = "发送订单模板邮件", description = "渲染 templates/mail/order.html，演示 th:each 列表迭代")
    public ApiResponse<Map<String, Object>> sendOrder(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "张三") String customer) {
        return ApiResponse.success(service.sendOrder(to, customer));
    }

    @GetMapping("/variables")
    @Operation(summary = "模板变量说明", description = "两个模板各自使用的变量清单")
    public ApiResponse<Map<String, Object>> variables() {
        return ApiResponse.success(service.variables());
    }
}

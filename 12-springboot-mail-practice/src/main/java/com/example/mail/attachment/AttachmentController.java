package com.example.mail.attachment;

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
 * 03. 附件邮件实验接口。
 */
@RestController
@RequestMapping("/api/attachment")
@RequiredArgsConstructor
@Tag(name = "03. 附件邮件", description = "文本/二进制附件、附件大小限制")
public class AttachmentController {

    private final AttachmentMailService service;

    @PostMapping("/csv")
    @Operation(summary = "发送 CSV 文本附件", description = "内存动态生成 CSV 并通过 addAttachment(InputStreamSource) 发送")
    public ApiResponse<Map<String, Object>> sendCsv(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【附件】订单导出") String subject,
            @RequestParam(defaultValue = "5") int rows) {
        return ApiResponse.success(service.sendCsv(to, subject, rows));
    }

    @PostMapping("/image")
    @Operation(summary = "发送 PNG 图片附件", description = "二进制附件，通过 addAttachment(DataSource) 发送")
    public ApiResponse<Map<String, Object>> sendImage(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【附件】统计图表") String subject) {
        return ApiResponse.success(service.sendImage(to, subject));
    }

    @GetMapping("/limitations")
    @Operation(summary = "附件大小限制", description = "各邮箱服务商附件配额说明")
    public ApiResponse<Map<String, Object>> limitations() {
        return ApiResponse.success(service.limitations());
    }
}

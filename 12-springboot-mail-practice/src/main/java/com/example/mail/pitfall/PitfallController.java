package com.example.mail.pitfall;

import com.example.mail.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 10. 常见坑与调优实验接口。
 */
@RestController
@RequestMapping("/api/pitfall")
@RequiredArgsConstructor
@Tag(name = "10. 常见坑与调优", description = "中文乱码 / 附件名 / 内联图片 / 异步丢异常 / 超时调优")
public class PitfallController {

    private final PitfallMailService service;

    @GetMapping("/list")
    @Operation(summary = "常见坑清单", description = "10 个高频问题的原因与解决方案")
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/plain-vs-html")
    @Operation(summary = "HTML 当纯文本演示", description = "setText(content, true/false) 的差异对比")
    public ApiResponse<Map<String, Object>> demoPlainVsHtml() {
        return ApiResponse.success(service.demoPlainVsHtml());
    }

    @GetMapping("/tuning")
    @Operation(summary = "超时与调优参数", description = "SMTP 超时 / TLS / 工程化要点")
    public ApiResponse<Map<String, Object>> tuning() {
        return ApiResponse.success(service.tuning());
    }
}

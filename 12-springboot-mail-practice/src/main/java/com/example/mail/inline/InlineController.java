package com.example.mail.inline;

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
 * 04. 内联图片邮件实验接口。
 */
@RestController
@RequestMapping("/api/inline")
@RequiredArgsConstructor
@Tag(name = "04. 内联图片", description = "cid 内联图片 / 内联 vs 外链")
public class InlineController {

    private final InlineMailService service;

    @PostMapping("/send")
    @Operation(summary = "发送带内联图片的邮件", description = "addInline(cid, DataSource) + <img src=\"cid:xxx\">")
    public ApiResponse<Map<String, Object>> send(
            @RequestParam(defaultValue = "zsx-receiver@example.com") String to,
            @RequestParam(defaultValue = "【内联图片】活动横幅") String subject) {
        return ApiResponse.success(service.sendInline(to, subject));
    }

    @GetMapping("/compare")
    @Operation(summary = "内联 vs 外链图片", description = "两种做法优劣对比")
    public ApiResponse<Map<String, Object>> compare() {
        return ApiResponse.success(service.compare());
    }
}

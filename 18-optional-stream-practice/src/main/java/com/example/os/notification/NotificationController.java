package com.example.os.notification;

import com.example.os.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 06 消息通知过滤接口。
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/filter")
    @Operation(summary = "消息通知过滤", description = "按用户、类型、已读状态、保留天数过滤通知，演示 Optional.ifPresent + Stream。")
    public ApiResponse<Map<String, Object>> filter(
            @Parameter(description = "用户 ID，为空查全部", example = "1")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "通知类型，为空查全部", example = "PROMOTION")
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(notificationService.filter(userId, type));
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(notificationService.explain());
    }
}

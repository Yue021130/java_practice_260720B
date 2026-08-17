package com.example.os.userprofile;

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
 * 01 用户画像聚合接口。
 */
@RestController
@RequestMapping("/api/userprofile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/aggregate")
    @Operation(summary = "聚合用户画像", description = "根据用户 ID 拉取会员信息并聚合订单数据，仅 VIP 且邮箱有效才返回完整画像。")
    public ApiResponse<Map<String, Object>> aggregate(
            @Parameter(description = "用户 ID", example = "1")
            @RequestParam Long userId) {
        return ApiResponse.ok(userProfileService.aggregate(userId));
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(userProfileService.explain());
    }
}

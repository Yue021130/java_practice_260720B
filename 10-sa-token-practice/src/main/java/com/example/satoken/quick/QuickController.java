package com.example.satoken.quick;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Quick 快速登录：一键登录 / 手机号验证码登录等简化登录场景。
 */
@RestController
@RequestMapping("/api/quick")
public class QuickController {

    /**
     * 模拟手机号一键登录：只要手机号格式正确即视为登录成功。
     */
    @PostMapping("/phone-login")
    public ApiResponse<Map<String, Object>> phoneLogin(@RequestParam String phone) {
        if (phone == null || !phone.matches("1[3-9]\\d{9}")) {
            return ApiResponse.error(400, "手机号格式错误");
        }
        String loginId = "phone:" + phone;
        StpUtil.login(loginId);
        Map<String, Object> data = new HashMap<>();
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("loginId", loginId);
        data.put("tip", "Quick 登录省略了账号密码，适用于手机号/邮箱/扫码等一键登录");
        return ApiResponse.success("Quick 登录成功", data);
    }

    /**
     * 模拟扫码登录：传入扫码得到的临时 code 换取登录态。
     */
    @PostMapping("/scan-login")
    public ApiResponse<Map<String, Object>> scanLogin(@RequestParam String scanCode) {
        if (!"SCAN-OK".equals(scanCode)) {
            return ApiResponse.error(400, "扫码码无效");
        }
        StpUtil.login("scan-user");
        Map<String, Object> data = new HashMap<>();
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("loginId", StpUtil.getLoginId());
        return ApiResponse.success("扫码登录成功", data);
    }

    /**
     * Quick 登录说明。
     */
    @GetMapping("/intro")
    public ApiResponse<String> intro() {
        return ApiResponse.success("Quick 登录通常与 OAuth2 / 短信平台 / 微信扫码结合，把外部认证结果映射为 Sa-Token 登录态");
    }
}

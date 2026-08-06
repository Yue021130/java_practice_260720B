package com.example.satoken.advanced;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 高级认证：二级认证、身份切换、模拟他人、多账号、加密。
 */
@RestController
@RequestMapping("/api/advanced")
public class AdvancedController {

    private static final String AES_KEY = "1234567890123456";

    /**
     * 二级认证：开启安全认证（例如支付密码校验后开启 5 分钟有效期）。
     */
    @PostMapping("/second-auth")
    public ApiResponse<Void> openSafe(@RequestParam(defaultValue = "pay") String service) {
        StpUtil.openSafe(service, 300);
        return ApiResponse.success("二级认证已开启（" + service + "，300 秒有效）", null);
    }

    /**
     * 校验二级认证。
     */
    @GetMapping("/check-safe")
    public ApiResponse<Void> checkSafe(@RequestParam(defaultValue = "pay") String service) {
        StpUtil.checkSafe(service);
        return ApiResponse.success("二级认证校验通过：" + service, null);
    }

    /**
     * 临时身份切换：把当前会话临时切换到指定账号。
     */
    @PostMapping("/switch-to")
    public ApiResponse<Map<String, Object>> switchTo(@RequestParam(defaultValue = "10002") Long id) {
        StpUtil.switchTo(id, () -> {
            // 在切换后的身份下执行逻辑
        });
        Map<String, Object> data = new HashMap<>();
        data.put("originalLoginId", StpUtil.getLoginId());
        data.put("switchedTo", id);
        return ApiResponse.success("临时身份切换完成（当前已恢复）", data);
    }

    /**
     * 模拟他人账号：操作指定账号的 Session。
     */
    @PostMapping("/mock")
    public ApiResponse<Object> mockSession(
            @RequestParam(defaultValue = "10002") Long id,
            @RequestParam String key) {
        Object value = StpUtil.getSessionByLoginId(id).get(key);
        return ApiResponse.success("账号 " + id + " 的 Session[" + key + "] = " + value);
    }

    /**
     * 多账号体系登录：使用 Admin 体系登录。
     */
    @PostMapping("/login-admin")
    public ApiResponse<Map<String, Object>> loginAdmin(@RequestParam(defaultValue = "admin-1") String id) {
        AdminStpUtil.login(id);
        Map<String, Object> data = new HashMap<>();
        data.put("adminToken", AdminStpUtil.getTokenValue());
        data.put("adminLoginId", AdminStpUtil.getLoginId());
        return ApiResponse.success("Admin 体系登录成功", data);
    }

    /**
     * 多账号体系校验：查询 Admin 是否登录。
     */
    @GetMapping("/admin-is-login")
    public ApiResponse<Boolean> adminIsLogin() {
        return ApiResponse.success(AdminStpUtil.isLogin());
    }

    /**
     * 密码加密演示：MD5 / SHA256 / AES。
     */
    @PostMapping("/encrypt")
    public ApiResponse<Map<String, String>> encrypt(@RequestParam(defaultValue = "123456") String password) {
        Map<String, String> data = new HashMap<>();
        data.put("md5", SaSecureUtil.md5(password));
        data.put("sha256", SaSecureUtil.sha256(password));
        data.put("aesEncrypt", SaSecureUtil.aesEncrypt(password, AES_KEY));
        data.put("note", "AES 解密需在业务层妥善保存密钥，避免 URL 传输破坏 Base64 密文");
        return ApiResponse.success(data);
    }
}

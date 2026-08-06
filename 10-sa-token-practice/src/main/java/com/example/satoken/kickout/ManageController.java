package com.example.satoken.kickout;

import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 账号管理：踢人下线 / 强制注销 / 账号封禁。
 */
@RestController
@RequestMapping("/api/manage")
public class ManageController {

    private static final String DISABLE_CATEGORY = "comment";
    private static final String DISABLE_LEVEL_KEY = "login-fail";

    /**
     * 按账号 id 踢下线（Token 失效，但用户再次请求会重新登录）。
     */
    @PostMapping("/kickout")
    public ApiResponse<Void> kickout(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.kickout(id);
        return ApiResponse.success("账号 " + id + " 已被踢下线", null);
    }

    /**
     * 按 Token 值踢下线。
     */
    @PostMapping("/kickout-by-token")
    public ApiResponse<Void> kickoutByToken(@RequestParam String tokenValue) {
        StpUtil.kickoutByTokenValue(tokenValue);
        return ApiResponse.success("指定 Token 已被踢下线", null);
    }

    /**
     * 强制注销：清除指定账号的所有登录状态。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> forceLogout(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.logout(id);
        return ApiResponse.success("账号 " + id + " 已被强制注销", null);
    }

    /**
     * 账号封禁 300 秒。
     */
    @PostMapping("/disable")
    public ApiResponse<Void> disable(@RequestParam(defaultValue = "10001") Long id) {
        StpUtil.disable(id, 300);
        return ApiResponse.success("账号 " + id + " 已被封禁 300 秒", null);
    }

    /**
     * 按业务分类封禁（例如：禁止评论）。
     */
    @PostMapping("/disable-category")
    public ApiResponse<Void> disableCategory(
            @RequestParam(defaultValue = "10001") Long id,
            @RequestParam(defaultValue = "300") long seconds) {
        StpUtil.disableLevel(id, DISABLE_CATEGORY, 1, seconds);
        return ApiResponse.success("账号 " + id + " 在 " + DISABLE_CATEGORY + " 业务被封禁 " + seconds + " 秒", null);
    }

    /**
     * 阶梯封禁：处罚次数越多，封禁时间越长。
     */
    @PostMapping("/disable-level")
    public ApiResponse<Map<String, Object>> disableLevel(
            @RequestParam(defaultValue = "10001") Long id,
            @RequestParam(defaultValue = "1") int level) {
        long seconds = level * 60L;
        StpUtil.disableLevel(id, DISABLE_LEVEL_KEY, level, seconds);
        Map<String, Object> data = new HashMap<>();
        data.put("level", level);
        data.put("seconds", seconds);
        return ApiResponse.success("阶梯封禁成功", data);
    }

    /**
     * 查询账号封禁状态。
     */
    @GetMapping("/is-disable")
    public ApiResponse<Map<String, Object>> isDisable(@RequestParam(defaultValue = "10001") Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("isDisable", StpUtil.isDisable(id));
        data.put("disableTime", StpUtil.getDisableTime(id));
        data.put("isDisableCategory", StpUtil.isDisable(id, DISABLE_CATEGORY));
        data.put("isDisableLevel", StpUtil.isDisableLevel(id, DISABLE_LEVEL_KEY, 1));
        return ApiResponse.success(data);
    }
}

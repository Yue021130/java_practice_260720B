package com.example.satoken.permission;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限认证：权限码、角色、注解鉴权、越级授权。
 */
@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    /**
     * 登录并写入权限码 / 角色。
     */
    @PostMapping("/login-with-perms")
    public ApiResponse<Map<String, Object>> loginWithPerms(
            @RequestParam(defaultValue = "10001") Long id,
            @RequestParam(defaultValue = "user:add,user:edit") String perms,
            @RequestParam(defaultValue = "user") String roles) {
        StpUtil.login(id);
        List<String> permList = Arrays.asList(perms.split(","));
        List<String> roleList = Arrays.asList(roles.split(","));
        StpUtil.getSession().set("permList", permList);
        StpUtil.getSession().set("roleList", roleList);
        Map<String, Object> data = new HashMap<>();
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("perms", permList);
        data.put("roles", roleList);
        return ApiResponse.success("登录并写入权限/角色成功", data);
    }

    /**
     * 代码层校验权限。
     */
    @GetMapping("/check-perm")
    public ApiResponse<Void> checkPermission(@RequestParam String perm) {
        StpUtil.checkPermission(perm);
        return ApiResponse.success("具备权限：" + perm, null);
    }

    /**
     * 注解鉴权：需要 admin 权限。
     */
    @SaCheckPermission("admin")
    @GetMapping("/anno-admin")
    public ApiResponse<String> annoAdmin() {
        return ApiResponse.success("admin 权限校验通过");
    }

    /**
     * 注解鉴权（AND 模式）：需要同时具备 a 和 b 权限。
     */
    @SaCheckPermission(value = {"a", "b"}, mode = SaMode.AND)
    @GetMapping("/anno-and")
    public ApiResponse<String> annoAnd() {
        return ApiResponse.success("同时具备 a、b 权限");
    }

    /**
     * 注解鉴权（OR 模式）：具备 a 或 b 任一权限即可。
     */
    @SaCheckPermission(value = {"a", "b"}, mode = SaMode.OR)
    @GetMapping("/anno-or")
    public ApiResponse<String> annoOr() {
        return ApiResponse.success("具备 a 或 b 权限");
    }

    /**
     * 角色认证：需要 admin 角色。
     */
    @SaCheckRole("admin")
    @GetMapping("/check-role")
    public ApiResponse<String> checkRole(@RequestParam(defaultValue = "admin") String role) {
        return ApiResponse.success("角色校验通过：" + role);
    }

    /**
     * 越级授权：给当前会话临时增加权限。
     */
    @PostMapping("/grant")
    public ApiResponse<Void> grantPermission(@RequestParam String perm) {
        StpUtil.getSession().set("permList", Arrays.asList(perm));
        return ApiResponse.success("已临时授予权限：" + perm, null);
    }
}

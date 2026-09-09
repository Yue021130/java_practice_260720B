package com.example.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.rbac.common.Result;
import com.example.rbac.dto.LoginDTO;
import com.example.rbac.dto.LoginVO;
import com.example.rbac.dto.PermissionVO;
import com.example.rbac.service.AuthService;
import com.example.rbac.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口：登录（公开）、用户信息、菜单树（动态路由数据源）、登出
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PermissionService permissionService;

    public AuthController(AuthService authService, PermissionService permissionService) {
        this.authService = authService;
        this.permissionService = permissionService;
    }

    @Operation(summary = "登录", description = "校验用户名密码，签发 Sa-Token JWT")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @Operation(summary = "当前用户信息（含角色与权限点）")
    @SaCheckLogin
    @GetMapping("/info")
    public Result<LoginVO> info() {
        return Result.ok(authService.currentUser());
    }

    @Operation(summary = "当前用户可见菜单树", description = "前端据此动态生成路由")
    @SaCheckLogin
    @GetMapping("/menus")
    public Result<List<PermissionVO>> menus() {
        return Result.ok(permissionService.menuTreeByUserId(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "退出登录")
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }
}

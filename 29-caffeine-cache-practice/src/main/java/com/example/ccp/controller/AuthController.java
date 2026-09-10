package com.example.ccp.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.example.ccp.common.Result;
import com.example.ccp.dto.LoginDTO;
import com.example.ccp.dto.LoginVO;
import com.example.ccp.dto.UserVO;
import com.example.ccp.entity.User;
import com.example.ccp.service.AuthService;
import org.springframework.beans.BeanUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录（放行）、登出、当前用户信息（需登录）。
 */
@Tag(name = "认证管理", description = "登录、登出、当前用户信息")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @SaIgnore
    @Operation(summary = "登录", description = "校验用户名密码，成功返回 JWT token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "当前用户信息", description = "从 JWT 中解析登录人并返回基本信息")
    @GetMapping("/info")
    public Result<UserVO> info() {
        User user = authService.currentUser();
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return Result.ok(vo);
    }
}

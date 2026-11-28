package com.example.prod.controller;

import com.example.prod.dto.LoginDTO;
import com.example.prod.dto.LoginVO;
import com.example.prod.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 认证接口：登录（公开）、登出、当前用户
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录", description = "校验用户名密码，签发 JWT；失败返回统一 401")
    @PostMapping("/login")
    public LoginVO login(@Validated @RequestBody LoginDTO dto, HttpServletRequest request) {
        return authService.login(dto, clientIp(request));
    }

    @Operation(summary = "退出登录", description = "解析 Authorization 头，拉黑 token 的 jti")
    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return "ok";
    }

    @Operation(summary = "当前用户信息", description = "从 SecurityContext 取用户名查库")
    @GetMapping("/me")
    public LoginVO me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return authService.me(username);
    }

    /**
     * 客户端 IP：优先 X-Forwarded-For（经过反向代理时），取第一个；否则 remoteAddr
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}

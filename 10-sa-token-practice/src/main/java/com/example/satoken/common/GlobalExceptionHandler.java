package com.example.satoken.common;

import cn.dev33.satoken.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * 将 Sa-Token 抛出的各类鉴权异常转换为统一 API 响应，并设置对应 HTTP 状态码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLoginException(NotLoginException e) {
        log.warn("未登录或登录已过期: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "未登录或登录已过期：" + e.getMessage()));
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<ApiResponse<Void>> handleNoAuthException(SaTokenException e) {
        String msg;
        if (e instanceof NotPermissionException) {
            msg = "无权限：" + ((NotPermissionException) e).getPermission();
            log.warn("无权限: {}", ((NotPermissionException) e).getPermission());
        } else {
            msg = "无角色：" + ((NotRoleException) e).getRole();
            log.warn("无角色: {}", ((NotRoleException) e).getRole());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, msg));
    }

    @ExceptionHandler(NotSafeException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotSafeException(NotSafeException e) {
        log.warn("二级认证未通过: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponse.error(402, "二级认证未通过：" + e.getMessage()));
    }

    @ExceptionHandler(DisableServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisableServiceException(DisableServiceException e) {
        log.warn("账号被封禁: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "账号已被封禁：" + e.getMessage()));
    }

    @ExceptionHandler(SaTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleSaTokenException(SaTokenException e) {
        log.warn("Sa-Token 异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Sa-Token 异常：" + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "系统异常：" + e.getMessage()));
    }
}

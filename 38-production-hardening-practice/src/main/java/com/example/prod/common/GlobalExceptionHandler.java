package com.example.prod.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理：统一错误格式 {code, message}，配合 HTTP 状态码
 *
 * <p>对照表：
 * BadCredentialsException（登录失败）            -> 401
 * AccessDeniedException（@PreAuthorize 拒绝）    -> 403
 * NoHandlerFoundException / IllegalArgumentException / 参数校验失败 -> 400
 * BusinessException / RuntimeException           -> 400</p>
 *
 * <p>说明：Boot 3 的 NoResourceFoundException 在 Spring Boot 2.7 不存在，
 * 其对应物是 NoHandlerFoundException（需 spring.mvc.throw-exception-if-no-handler-found=true
 * 才会抛出），这里兜底 404。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        log.warn("[认证失败] {}", e.getMessage());
        return build(401, 401, "用户名或密码错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[越权访问] {}", e.getMessage());
        return build(403, 403, "无权限执行该操作");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        log.warn("[业务异常] code={} msg={}", e.getCode(), e.getMessage());
        return build(400, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败"
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        log.warn("[参数校验] {}", msg);
        return build(400, 400, msg);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandler(NoHandlerFoundException e) {
        log.warn("[404] {} {}", e.getHttpMethod(), e.getRequestURL());
        return build(404, 404, "接口不存在");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[非法参数] {}", e.getMessage());
        return build(400, 400, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        log.warn("[运行时异常] {}", e.getMessage());
        return build(400, 400, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(int httpStatus, int code, String message) {
        Map<String, Object> body = new HashMap<>(4);
        body.put("code", code);
        body.put("message", message);
        return ResponseEntity.status(httpStatus).body(body);
    }
}

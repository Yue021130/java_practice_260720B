package com.example.cache.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring 全局异常处理。
 *
 * 统一拦截 Controller 抛出的异常，转换为 ApiResponse，避免异常堆栈直接暴露给前端。
 * 大部分「失败」其实是教学演示（如故意访问不存在的 key），都在 Service 内部捕获
 * 并转成可读文本返回；这里只兜底真正的异常。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 缓存业务异常：已知错误，返回错误码与提示。
     */
    @ExceptionHandler(CacheBizException.class)
    public ResponseEntity<ApiResponse<Void>> handleCacheBizException(CacheBizException e) {
        log.warn("缓存业务异常：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验异常：@RequestBody 形式。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400001, "参数校验失败", errors));
    }

    /**
     * 兜底：未知异常，记录完整堆栈，对外脱敏。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("系统异常：", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500000, "系统繁忙，请稍后重试"));
    }
}

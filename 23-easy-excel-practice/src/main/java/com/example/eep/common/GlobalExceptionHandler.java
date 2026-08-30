package com.example.eep.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ApiResponse.fail(400, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalState(IllegalStateException e) {
        log.warn("状态错误: {}", e.getMessage());
        return ApiResponse.fail(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.fail("系统繁忙，请稍后再试");
    }
}

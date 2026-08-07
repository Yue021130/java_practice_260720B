package com.example.threadpooladvanced.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.RejectedExecutionException;

/**
 * 全局异常处理：把线程池拒绝等异常包装成统一响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RejectedExecutionException.class)
    public ApiResponse<Void> handleRejected(RejectedExecutionException e) {
        log.warn("线程池拒绝任务: {}", e.getMessage());
        return ApiResponse.fail(503, "线程池已饱和，任务被拒绝: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数非法: {}", e.getMessage());
        return ApiResponse.fail(400, "参数非法: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.fail(500, "系统异常: " + e.getMessage());
    }
}

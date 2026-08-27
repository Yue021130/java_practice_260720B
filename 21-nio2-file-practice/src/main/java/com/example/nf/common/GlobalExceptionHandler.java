package com.example.nf.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;

/**
 * 全局异常处理：把 IO 相关异常包装成统一响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchFileException.class)
    public ApiResponse<Void> handleNoSuchFile(NoSuchFileException e) {
        log.warn("文件不存在: {}", e.getMessage());
        return ApiResponse.fail(404, "文件不存在: " + e.getMessage());
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    public ApiResponse<Void> handleFileAlreadyExists(FileAlreadyExistsException e) {
        log.warn("文件已存在: {}", e.getMessage());
        return ApiResponse.fail(409, "文件已存在: " + e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ApiResponse<Void> handleSecurity(SecurityException e) {
        log.warn("路径越界: {}", e.getMessage());
        return ApiResponse.fail(403, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ApiResponse.fail(400, e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ApiResponse<Void> handleIOException(IOException e) {
        log.error("IO 异常", e);
        return ApiResponse.fail("IO 异常: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.fail("系统繁忙，请稍后再试");
    }
}

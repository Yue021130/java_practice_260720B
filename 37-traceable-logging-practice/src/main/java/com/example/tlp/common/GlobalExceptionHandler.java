package com.example.tlp.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理：异常链路也要可追溯。
 *
 * <p>异常栈由日志框架在打 ERROR 日志时输出，处理它的日志和
 * 触发它的请求在同一个 MDC 上下文里——traceId 天然一致，
 * 这正是「错误日志按 traceId 一捞一个准」的原因。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handle(Exception e, HttpServletRequest request) {
        // 这里打的 ERROR 与业务日志同 traceId，会同时进 current.log 和 error-current.log
        log.error("接口 [{} {}] 处理异常", request.getMethod(), request.getRequestURI(), e);
        return "ERROR: " + e.getMessage();
    }
}

package com.example.ts.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理
 *
 * <p>八股：@RestControllerAdvice + @ExceptionHandler 的拦截范围？
 * 1. 拦截 Controller 层抛出的异常；
 * 2. 不会拦截过滤器 Filter、拦截器 Interceptor 中抛出的异常（需要单独处理）；
 * 3. 对 @Valid 校验异常单独处理，返回具体字段错误提示。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 uri={} msg={}", request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidException(Exception e, HttpServletRequest request) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException) {
            msg = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getDefaultMessage())
                    .findFirst()
                    .orElse(msg);
        } else if (e instanceof BindException) {
            msg = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getDefaultMessage())
                    .findFirst()
                    .orElse(msg);
        }
        log.warn("参数校验异常 uri={} msg={}", request.getRequestURI(), msg);
        return Result.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 uri={}", request.getRequestURI(), e);
        return Result.fail("系统繁忙，请稍后再试");
    }
}

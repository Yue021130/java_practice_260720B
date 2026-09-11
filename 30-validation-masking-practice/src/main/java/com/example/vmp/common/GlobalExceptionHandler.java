package com.example.vmp.common;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理：Controller 层无需 try-catch，所有异常在这里统一转成 Result。
 *
 * <p>四类异常：</p>
 * <ul>
 *   <li>BusinessException —— 业务错误，返回自定义 code/msg；</li>
 *   <li>NotLoginException —— Sa-Token 未登录（含 Token 失效），返回 401，前端拦截器据此跳登录页；</li>
 *   <li>参数校验异常 —— MethodArgumentNotValidException（@RequestBody）、BindException、
 *       ConstraintViolationException（方法参数）统一返回 400；</li>
 *   <li>未知异常 —— 返回 500。</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Sa-Token 未登录：前端 axios 响应拦截器识别 code=401 后清除状态并跳转登录页 */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录或登录已过期: {}", e.getMessage());
        return Result.fail(401, "未登录或登录已过期，请重新登录");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 参数校验失败（JSR-303） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.fail(400, msg);
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        return Result.fail(400, msg);
    }

    /** 方法参数上的校验注解失败（@RequestParam / 普通参数），与 @RequestBody 的异常类型不同 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("参数校验失败");
        return Result.fail(400, msg);
    }

    /** 兜底异常：不要把堆栈细节暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后再试");
    }
}

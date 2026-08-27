package com.example.ur.common.advice;

import com.example.ur.common.result.BusinessException;
import com.example.ur.common.result.Result;
import com.example.ur.common.result.ResultCode;
import com.example.ur.common.result.ResultFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>保证任何异常都以统一的 Result 结构返回给前端，不暴露原始堆栈。
 * 兜底文案给前端，堆栈留给自己查日志。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 自定义业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResultFactory.failed(e.getCode(), e.getMessage());
    }

    /** 参数校验异常（@Valid 校验失败） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        log.warn("参数校验失败: {}", msg);
        return ResultFactory.failed(ResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    /** 参数绑定异常 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        log.warn("参数绑定失败: {}", msg);
        return ResultFactory.failed(ResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    /** 兜底异常：未知异常统一提示，避免把内部细节暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ResultFactory.failed(ResultCode.SYSTEM_ERROR);
    }
}

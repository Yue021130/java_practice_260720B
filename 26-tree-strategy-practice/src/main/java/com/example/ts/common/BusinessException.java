package com.example.ts.common;

/**
 * 业务异常
 *
 * <p>使用场景：参数校验失败、业务规则不满足（如余额不足、支付方式不支持）等。
 * 由 GlobalExceptionHandler 捕获并包装成 Result.fail 返回。</p>
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}

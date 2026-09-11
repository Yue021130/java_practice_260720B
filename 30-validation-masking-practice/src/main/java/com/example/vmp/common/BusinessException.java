package com.example.vmp.common;

import lombok.Getter;

/**
 * 业务异常：Service 层校验失败时抛出，由 GlobalExceptionHandler 统一捕获转成 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，与 Result.code 对应 */
    private final Integer code;

    public BusinessException(String msg) {
        this(500, msg);
    }

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
}

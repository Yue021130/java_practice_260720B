package com.example.prod.common;

import lombok.Getter;

/**
 * 业务异常：带 HTTP 语义码，由 GlobalExceptionHandler 统一转成错误响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}

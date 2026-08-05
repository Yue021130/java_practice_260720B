package com.example.exception.hierarchy;

import lombok.Getter;

/**
 * 自定义业务异常。
 *
 * 继承 RuntimeException，属于 unchecked exception。
 * 特点：
 * 1. 携带业务错误码（errorCode）；
 * 2. 支持 cause 保留底层异常；
 * 3. 可被 @ControllerAdvice 统一捕获。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

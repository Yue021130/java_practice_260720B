package com.example.ur.common.result;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>业务代码里主动抛出，由全局异常处理器统一处理并转成 Result。
 * 相比直接抛 RuntimeException，它携带了业务状态码，能被前端识别。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}

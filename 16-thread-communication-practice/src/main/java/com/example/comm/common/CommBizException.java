package com.example.comm.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线程通信实验中的业务异常。
 *
 * 已知问题（参数非法 / 等待超时等）都转成它，
 * 由全局异常处理器转成可读的 ApiResponse 返回，不让堆栈直接暴露给前端。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommBizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private int code;

    public CommBizException(String message) {
        this(400100, message);
    }

    public CommBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CommBizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400100;
    }
}

package com.example.unsafe.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Unsafe 实验中的业务异常。
 *
 * 与 {@link sun.misc.Unsafe} 本身不同，业务异常是“已知问题”：
 * 抛出后由全局异常处理器转成可读的 ApiResponse 返回，不会让 JVM 崩溃。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnsafeBizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private int code;

    public UnsafeBizException(String message) {
        this(400100, message);
    }

    public UnsafeBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public UnsafeBizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400100;
    }
}

package com.example.cache.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 缓存实验中的业务异常。
 *
 * 已知问题（缓存 key 不存在 / 参数非法等）都转成它，
 * 由全局异常处理器转成可读的 ApiResponse 返回，不让堆栈直接暴露给前端。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CacheBizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private int code;

    public CacheBizException(String message) {
        this(400100, message);
    }

    public CacheBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CacheBizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400100;
    }
}

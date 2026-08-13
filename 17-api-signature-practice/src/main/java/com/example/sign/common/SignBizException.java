package com.example.sign.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接口签名鉴权实验中的业务异常。
 *
 * 已知问题（签名缺失 / 时间戳过期 / nonce 重复 / 签名不匹配等）都转成它，
 * 由全局异常处理器转成可读的 ApiResponse 返回，不让堆栈直接暴露给前端。
 * 真实工程里，鉴权失败一般直接返回 401 而不是业务码——教学项目保留可读性。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SignBizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private int code;

    public SignBizException(String message) {
        this(400100, message);
    }

    public SignBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SignBizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400100;
    }
}

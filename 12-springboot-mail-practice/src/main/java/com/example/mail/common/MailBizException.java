package com.example.mail.common;

/**
 * 邮件业务异常。
 *
 * 携带错误码与用户可读的提示，由 {@link GlobalExceptionHandler} 统一转换为 400 响应。
 */
public class MailBizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private final int code;

    public MailBizException(String message) {
        super(message);
        this.code = 400100;
    }

    public MailBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MailBizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400100;
    }

    public int getCode() {
        return code;
    }
}

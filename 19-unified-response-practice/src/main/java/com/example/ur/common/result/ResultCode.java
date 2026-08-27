package com.example.ur.common.result;

import lombok.Getter;

/**
 * 业务状态码枚举。
 *
 * <p>集中管理状态码，禁止在业务代码里散落魔法数字。
 * HTTP 200 只表示"网络传输层没问题"，业务是否成功由 code 表达。</p>
 */
@Getter
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(0, "操作成功"),

    /** 通用失败 */
    FAILED(500, "操作失败"),

    /** 参数校验失败 */
    VALIDATE_FAILED(400, "参数校验失败"),

    /** 暂未登录或登录已过期 */
    UNAUTHORIZED(401, "暂未登录或登录已过期"),

    /** 没有相关权限 */
    FORBIDDEN(403, "没有相关权限"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 系统繁忙，请稍后再试 */
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

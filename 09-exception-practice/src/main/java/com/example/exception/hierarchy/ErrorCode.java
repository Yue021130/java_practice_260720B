package com.example.exception.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举。
 *
 * 用于演示 Spring 全局异常处理中“业务异常码”的设计思路。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "成功"),
    PARAM_INVALID(400001, "参数非法"),
    RESOURCE_NOT_FOUND(404001, "资源不存在"),
    ORDER_STATUS_ERROR(500001, "订单状态不正确"),
    SYSTEM_ERROR(500000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;
}

package com.example.ts.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * <p>八股：为什么需要统一返回？
 * 1. 前端拦截器可以统一判断 code；
 * 2. 日志、AOP、全局异常处理都能基于固定结构做处理；
 * 3. 团队协作时接口语义一致，降低沟通成本。
 * </p>
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码，0 表示成功 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 数据载荷 */
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}

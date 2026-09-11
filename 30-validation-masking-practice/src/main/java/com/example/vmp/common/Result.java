package com.example.vmp.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果：所有 Controller 出参都包装成 Result，前端 axios 响应拦截器统一解包。
 *
 * <p>八股：为什么需要统一返回？—— 前端只需关心 {code, msg, data} 三种结构，
 * 配合全局异常处理，业务错误与系统错误格式一致，降低前后端联调成本。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    /** 状态码：0 成功，非 0 失败（401 未登录、400 参数错误、500 系统错误等） */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 业务数据 */
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        return fail(500, msg);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}

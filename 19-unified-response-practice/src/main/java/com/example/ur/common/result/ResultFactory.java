package com.example.ur.common.result;

/**
 * Result 静态工厂方法。
 *
 * <p>业务代码不需要自己 new Result，调用方代码更简洁、意图更清晰。</p>
 */
public class ResultFactory {

    private ResultFactory() {
        // 工具类禁止实例化
    }

    /** 成功：无数据 */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功：带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功：带数据和自定义提示 */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /** 失败：使用枚举中预设的状态码和提示 */
    public static <T> Result<T> failed(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败：自定义提示信息（状态码用通用 FAILED） */
    public static <T> Result<T> failed(String msg) {
        return new Result<>(ResultCode.FAILED.getCode(), msg, null);
    }

    /** 失败：完全自定义状态码和提示 */
    public static <T> Result<T> failed(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}

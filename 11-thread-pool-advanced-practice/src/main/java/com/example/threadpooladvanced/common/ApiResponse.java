package com.example.threadpooladvanced.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一响应结构，与仓库其他模块保持一致。
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        r.setTimestamp(LocalDateTime.now().toString());
        return r;
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTimestamp(LocalDateTime.now().toString());
        return r;
    }
}

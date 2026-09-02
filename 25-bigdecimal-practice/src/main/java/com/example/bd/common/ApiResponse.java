package com.example.bd.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一响应包装类。
 *
 * @param <T> 业务数据类型
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

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(500);
        r.setMessage(message);
        r.setTimestamp(LocalDateTime.now().toString());
        return r;
    }
}

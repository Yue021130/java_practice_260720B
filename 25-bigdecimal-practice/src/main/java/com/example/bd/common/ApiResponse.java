package com.example.bd.common;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应包装类。
 *
 * @param <T> 业务数据类型
 */
@Data
public class ApiResponse<T> {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int code;
    private String message;
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        r.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        return r;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(500);
        r.setMessage(message);
        r.setData(data);
        r.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        return r;
    }
}

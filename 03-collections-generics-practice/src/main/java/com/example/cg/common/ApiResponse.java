package com.example.cg.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 REST 接口返回结构。
 *
 * 无论成功或失败，都返回 code + message + data，方便前端统一处理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应体")
public class ApiResponse<T> {

    /** 状态码，200 表示成功 */
    @Schema(description = "状态码，200 表示成功", example = "200")
    private Integer code;

    /** 提示信息 */
    @Schema(description = "提示信息", example = "success")
    private String message;

    /** 业务数据 */
    @Schema(description = "业务数据")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

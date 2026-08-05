package com.example.exception.spring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户创建请求 DTO，用于演示参数校验异常。
 */
@Data
@Schema(description = "用户创建请求")
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度 2-20")
    @Schema(description = "用户名")
    private String username;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;
}

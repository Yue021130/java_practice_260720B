package com.example.prod.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应：JWT + 用户信息
 */
@Data
@Schema(description = "登录响应：token + 用户信息")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT，后续请求放 Authorization 请求头；/me 接口该字段为 null")
    private String token;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色：ADMIN / USER")
    private String role;

    /** token 类型默认值，前端组装请求头用 */
    @Schema(description = "token 类型，固定 Bearer")
    private String tokenType = "Bearer";
}

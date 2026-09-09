package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@Builder
@Schema(description = "登录响应：token + 用户信息 + 角色 + 权限点")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT token，后续请求放在 Authorization 请求头")
    private String token;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色编码列表")
    private Set<String> roles;

    @Schema(description = "权限点列表，如 order:delete")
    private Set<String> permissions;
}

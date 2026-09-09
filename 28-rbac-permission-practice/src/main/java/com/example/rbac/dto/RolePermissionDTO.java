package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "角色分配权限请求")
public class RolePermissionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "权限点不能为空")
    @Schema(description = "权限点 ID 列表")
    private List<Long> permissionIds;
}

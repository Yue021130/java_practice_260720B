package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "角色新增/编辑请求")
public class RoleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色 ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码，如 MANAGER")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String name;

    @NotNull(message = "数据权限范围不能为空")
    @Schema(description = "数据权限范围：1本人 2本部门 3全部")
    private Integer dataScope;

    @Schema(description = "权限点 ID 列表")
    private List<Long> permissionIds;
}

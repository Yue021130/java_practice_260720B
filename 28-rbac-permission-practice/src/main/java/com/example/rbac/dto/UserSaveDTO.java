package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "用户新增/编辑请求")
public class UserSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "登录名")
    private String username;

    @Schema(description = "密码（新增必填，编辑留空表示不修改）")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "昵称")
    private String nickname;

    @NotNull(message = "部门不能为空")
    @Schema(description = "部门 ID：1研发部 2运营部 3财务部")
    private Long deptId;

    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;
}

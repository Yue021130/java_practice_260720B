package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "用户分页记录")
public class UserPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "登录名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

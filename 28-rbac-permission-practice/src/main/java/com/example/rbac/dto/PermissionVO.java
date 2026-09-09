package com.example.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "权限树节点（menu 类型含前端路由信息）")
public class PermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "权限 ID")
    private Long id;

    @Schema(description = "父权限 ID")
    private Long parentId;

    @Schema(description = "权限编码")
    private String code;

    @Schema(description = "权限名称")
    private String name;

    /** 类型：menu 菜单 / button 按钮 */
    private String type;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "前端组件名")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    private Integer sort;

    @Schema(description = "子节点")
    private List<PermissionVO> children;
}

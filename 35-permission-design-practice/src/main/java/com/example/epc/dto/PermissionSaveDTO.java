package com.example.epc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 权限保存入参：新增 / 更新共用。
 */
@Data
public class PermissionSaveDTO implements Serializable {

    /** 更新时必传，新增为空 */
    private Long id;

    /** 父权限 id，顶级传 0 */
    private Long parentId;

    @NotBlank(message = "权限名称不能为空")
    private String name;

    /** 类型：MENU 菜单 / BUTTON 按钮 */
    @NotBlank(message = "权限类型不能为空")
    private String type;

    /** 权限点标识（BUTTON 用），如 user:list */
    private String perm;

    /** 前端路由路径（MENU 用） */
    private String path;

    private String icon;

    private Integer sortOrder;
}

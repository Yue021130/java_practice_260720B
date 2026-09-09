package com.example.ts.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 新增菜单请求 DTO
 */
@Data
public class MenuAddDTO {

    /** 父菜单 ID，0 表示顶级菜单 */
    @NotNull(message = "父菜单 ID 不能为空")
    private Long parentId;

    /** 菜单名称 */
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /** 路由路径 / 权限标识 */
    private String path;

    /** 排序，越小越靠前 */
    @NotNull(message = "排序不能为空")
    private Integer sort;

    /** 图标 */
    private String icon;
}

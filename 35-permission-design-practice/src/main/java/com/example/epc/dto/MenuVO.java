package com.example.epc.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单出参：登录后下发的可见菜单树（只含 MENU 类型权限，已按 sortOrder 排序）。
 * 前端据此渲染侧边栏与动态路由。
 */
@Data
public class MenuVO implements Serializable {

    private Long id;

    private Long parentId;

    private String name;

    /** 前端路由路径 */
    private String path;

    private String icon;

    private Integer sortOrder;

    private List<MenuVO> children;
}

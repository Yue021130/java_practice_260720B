package com.example.epc.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限出参：树节点结构（菜单/权限管理页渲染用）。
 */
@Data
public class PermissionVO implements Serializable {

    private Long id;

    private Long parentId;

    private String name;

    /** MENU / BUTTON */
    private String type;

    private String perm;

    private String path;

    private String icon;

    private Integer sortOrder;

    private List<PermissionVO> children;
}

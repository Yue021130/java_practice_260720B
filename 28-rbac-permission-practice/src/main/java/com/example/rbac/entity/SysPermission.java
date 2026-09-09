package com.example.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父权限 ID，0 为一级 */
    private Long parentId;

    /** 权限编码：menu:order / order:delete 等 */
    private String code;

    /** 权限名称 */
    private String name;

    /** 类型：menu 菜单 / button 按钮 */
    private String type;

    /** 前端路由路径（menu 类型） */
    private String path;

    /** 前端组件名（menu 类型） */
    private String component;

    /** 菜单图标 */
    private String icon;

    private Integer sort;

    @TableLogic
    private Integer deleted;
}

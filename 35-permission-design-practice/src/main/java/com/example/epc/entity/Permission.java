package com.example.epc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体，对应表 sys_permission。
 *
 * <p>权限分两类：MENU（菜单，登录后下发给前端渲染路由/菜单树）、
 * BUTTON（按钮，perm 字段即 @SaCheckPermission("user:list") 里的标识）。
 * 两者同表靠 type 区分，是「菜单即权限」模型的标准做法。</p>
 */
@Data
@TableName("sys_permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父权限 id，顶级为 0 */
    private Long parentId;

    /** 名称，如「用户管理」 */
    private String name;

    /** 类型：MENU 菜单 / BUTTON 按钮 */
    private String type;

    /** 权限点标识（BUTTON 用），如 user:list；MENU 可为空 */
    private String perm;

    /** 前端路由路径（MENU 用），如 /system/user */
    private String path;

    /** 图标（MENU 用），可空 */
    private String icon;

    /** 排序号，越小越靠前 */
    private Integer sortOrder;

    private LocalDateTime createTime;
}

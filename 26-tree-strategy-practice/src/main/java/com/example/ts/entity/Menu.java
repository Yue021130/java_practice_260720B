package com.example.ts.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单实体（对应数据库表 t_menu）
 *
 * <p>字段设计：
 * - id：主键，也是 Hutool TreeUtil 构建树的节点唯一标识；
 * - parentId：父菜单 ID，顶级菜单为 0；
 * - name：菜单名称；
 * - path：路由路径 / 权限标识；
 * - sort：排序权重，Hutool TreeUtil 中对应 weight 字段；
 * - icon：图标；
 * - createTime：创建时间。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private Integer sort;
    private String icon;
    private LocalDateTime createTime;
}

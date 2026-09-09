package com.example.ts.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单树形结构 VO
 *
 * <p>每个节点通过 children 字段递归挂载子节点，前端 el-tree / a-tree 可直接消费。</p>
 */
@Data
public class MenuTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private Integer sort;
    private String icon;
    private List<MenuTreeVO> children;
}

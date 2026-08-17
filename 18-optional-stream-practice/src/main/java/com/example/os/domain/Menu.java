package com.example.os.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单实体：演示 Optional.flatMap + Stream 递归构建权限树。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private Integer orderNum;

    /**
     * 子菜单列表：递归结构，使用 ArrayList 避免 NPE。
     */
    @Builder.Default
    private List<Menu> children = new ArrayList<>();
}

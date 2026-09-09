package com.example.ts.service;

import com.example.ts.dto.MenuAddDTO;
import com.example.ts.dto.MenuTreeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 菜单服务测试：验证 Hutool TreeUtil 构建树形结构
 */
@SpringBootTest
class MenuServiceTest {

    @Autowired
    private MenuService menuService;

    @Test
    void testTreeHasTopLevelAndChildren() {
        List<MenuTreeVO> tree = menuService.tree();
        // 初始化数据有 3 个顶级菜单
        assertEquals(3, tree.size());

        // 第一个顶级菜单是“系统管理”，它有 3 个子菜单
        MenuTreeVO system = tree.get(0);
        assertEquals("系统管理", system.getName());
        assertEquals(3, system.getChildren().size());

        // 用户管理下还有三级菜单“用户详情”
        MenuTreeVO user = system.getChildren().get(0);
        assertEquals("用户管理", user.getName());
        assertEquals(1, user.getChildren().size());
        assertEquals("用户详情", user.getChildren().get(0).getName());
    }

    @Test
    void testAddAndDelete() {
        int before = menuService.list().size();

        MenuAddDTO dto = new MenuAddDTO();
        dto.setParentId(0L);
        dto.setName("测试菜单");
        dto.setPath("/test");
        dto.setSort(99);
        menuService.add(dto);

        assertEquals(before + 1, menuService.list().size());

        Long id = menuService.list().stream()
                .filter(m -> "测试菜单".equals(m.getName()))
                .findFirst()
                .map(m -> m.getId())
                .orElseThrow(() -> new RuntimeException("未找到新增菜单"));
        menuService.delete(id);
        assertEquals(before, menuService.list().size());
    }
}

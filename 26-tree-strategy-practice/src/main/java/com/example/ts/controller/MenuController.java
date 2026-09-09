package com.example.ts.controller;

import com.example.ts.common.Result;
import com.example.ts.dto.MenuAddDTO;
import com.example.ts.dto.MenuTreeVO;
import com.example.ts.entity.Menu;
import com.example.ts.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理接口
 */
@RestController
@RequestMapping("/api/menu")
@Tag(name = "菜单管理", description = "基于 Hutool TreeUtil 构建菜单树")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    @Operation(summary = "新增菜单")
    public Result<Menu> add(@Validated @RequestBody MenuAddDTO dto) {
        return Result.ok(menuService.add(dto));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树")
    public Result<List<MenuTreeVO>> tree() {
        return Result.ok(menuService.tree());
    }

    @GetMapping("/list")
    @Operation(summary = "获取扁平菜单列表")
    public Result<List<Menu>> list() {
        return Result.ok(menuService.list());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单（含子菜单）")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}

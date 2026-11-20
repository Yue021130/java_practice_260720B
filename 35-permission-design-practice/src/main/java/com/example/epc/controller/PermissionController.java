package com.example.epc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.epc.common.Result;
import com.example.epc.dto.PermissionSaveDTO;
import com.example.epc.dto.PermissionVO;
import com.example.epc.entity.Permission;
import com.example.epc.permission.PermissionCache;
import com.example.epc.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限（菜单/按钮）管理接口。
 */
@Tag(name = "权限管理", description = "权限点 CRUD 与缓存刷新")
@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public PermissionController(PermissionService permissionService,
                                PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @SaCheckPermission("perm:list")
    @Operation(summary = "权限树", description = "全部权限点转树，供角色分配权限时勾选")
    @GetMapping("/tree")
    public Result<List<PermissionVO>> tree() {
        return Result.ok(permissionService.tree());
    }

    @SaCheckPermission("perm:list")
    @Operation(summary = "权限列表", description = "全部权限点平铺")
    @GetMapping("/list")
    public Result<List<Permission>> list() {
        return Result.ok(permissionService.list());
    }

    @SaCheckPermission("perm:create")
    @Operation(summary = "新增权限点")
    @PostMapping
    public Result<Void> save(@Validated @RequestBody PermissionSaveDTO dto) {
        permissionService.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("perm:update")
    @Operation(summary = "更新权限点")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody PermissionSaveDTO dto) {
        permissionService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("perm:delete")
    @Operation(summary = "删除权限点", description = "存在子权限时不允许删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return Result.ok();
    }

    @SaCheckPermission("perm:update")
    @Operation(summary = "刷新权限缓存", description = "手动全量失效 PermissionCache，权限数据异常时的兜底手段")
    @PostMapping("/refresh-cache")
    public Result<String> refreshCache() {
        permissionCache.evictAll();
        return Result.ok("已刷新");
    }
}

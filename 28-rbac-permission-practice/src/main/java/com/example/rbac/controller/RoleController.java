package com.example.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.rbac.common.Result;
import com.example.rbac.dto.RolePermissionDTO;
import com.example.rbac.dto.RoleSaveDTO;
import com.example.rbac.entity.SysRole;
import com.example.rbac.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口：角色 CRUD + 为角色分配权限点（权限可配置化）
 */
@Tag(name = "角色管理接口")
@RestController
@RequestMapping("/api/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色列表")
    @SaCheckPermission("role:list")
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.list());
    }

    @Operation(summary = "查询角色已拥有的权限点 ID")
    @SaCheckPermission("role:list")
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> permissionIds(@PathVariable Long id) {
        return Result.ok(roleService.listPermissionIds(id));
    }

    @Operation(summary = "新增/编辑角色（可携带权限点）")
    @SaCheckPermission("role:assign")
    @PostMapping
    public Result<Void> save(@Validated @RequestBody RoleSaveDTO dto) {
        roleService.save(dto);
        return Result.ok();
    }

    @Operation(summary = "为角色重新分配权限点", description = "分配后下一次请求立即生效（StpInterface 实时查库）")
    @SaCheckPermission("role:assign")
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @Validated @RequestBody RolePermissionDTO dto) {
        roleService.assignPermissions(id, dto.getPermissionIds());
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission("role:assign")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        roleService.remove(id);
        return Result.ok();
    }
}

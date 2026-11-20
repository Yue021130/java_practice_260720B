package com.example.epc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.epc.common.Result;
import com.example.epc.dto.RoleSaveDTO;
import com.example.epc.dto.RoleVO;
import com.example.epc.service.RoleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理接口：角色 CRUD + 权限点分配 + 数据范围配置。
 */
@Tag(name = "角色管理", description = "角色 CRUD、权限点分配、数据范围配置")
@RestController
@RequestMapping("/api/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @SaCheckPermission("role:list")
    @Operation(summary = "角色分页")
    @GetMapping("/page")
    public Result<Page<RoleVO>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) String roleName) {
        return Result.ok(roleService.page(current, size, roleName));
    }

    @SaCheckPermission("role:list")
    @Operation(summary = "角色详情", description = "含已分配权限点与数据范围规则，供编辑页回显")
    @GetMapping("/{id}")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    @SaCheckPermission("role:create")
    @Operation(summary = "新增角色", description = "创建角色、全量绑定权限点并写入数据范围规则")
    @PostMapping
    public Result<Void> save(@Validated @RequestBody RoleSaveDTO dto) {
        roleService.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("role:update")
    @Operation(summary = "更新角色", description = "更新基本信息，全量覆盖权限点并 upsert 数据范围规则")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody RoleSaveDTO dto) {
        roleService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("role:delete")
    @Operation(summary = "删除角色", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}

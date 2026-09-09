package com.example.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.rbac.common.Result;
import com.example.rbac.dto.PermissionVO;
import com.example.rbac.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限点接口：全量权限树，供角色管理页勾选分配
 */
@Tag(name = "权限点接口")
@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "全量权限树")
    @SaCheckPermission("role:list")
    @GetMapping("/tree")
    public Result<List<PermissionVO>> tree() {
        return Result.ok(permissionService.permissionTree());
    }
}

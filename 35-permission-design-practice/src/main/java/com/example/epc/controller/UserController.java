package com.example.epc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.epc.common.Result;
import com.example.epc.dto.UserSaveDTO;
import com.example.epc.dto.UserVO;
import com.example.epc.service.UserService;
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
 * 用户管理接口。
 */
@Tag(name = "用户管理", description = "用户 CRUD 与角色分配")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @SaCheckPermission("user:list")
    @Operation(summary = "用户分页")
    @GetMapping("/page")
    public Result<Page<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) String username) {
        return Result.ok(userService.page(current, size, username));
    }

    @SaCheckPermission("user:list")
    @Operation(summary = "用户详情", description = "含部门名与已分配角色，供编辑页回显")
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @SaCheckPermission("user:create")
    @Operation(summary = "新增用户", description = "创建用户并分配角色；密码不传默认 123456")
    @PostMapping
    public Result<Void> save(@Validated @RequestBody UserSaveDTO dto) {
        userService.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("user:update")
    @Operation(summary = "更新用户", description = "更新基本信息并全量覆盖角色；密码为空表示不修改")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody UserSaveDTO dto) {
        userService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("user:delete")
    @Operation(summary = "删除用户", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}

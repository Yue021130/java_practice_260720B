package com.example.ccp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ccp.common.Result;
import com.example.ccp.dto.UserSaveDTO;
import com.example.ccp.dto.UserVO;
import com.example.ccp.service.UserService;
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
 * 用户管理接口：分页列表、详情、新增、编辑、删除。全部需要登录。
 */
@Tag(name = "用户管理", description = "用户 CRUD，演示 Caffeine 缓存命中")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页列表", description = "命中 userPage 缓存时控制台不打印 SQL")
    @GetMapping("/page")
    public Result<Page<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) String username) {
        return Result.ok(userService.page(current, size, username));
    }

    @Operation(summary = "用户详情", description = "命中 user 缓存时控制台不打印 SQL")
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<UserVO> save(@Validated @RequestBody UserSaveDTO dto) {
        return Result.ok(userService.save(dto));
    }

    @Operation(summary = "编辑用户", description = "密码留空表示不修改；成功后清空相关缓存")
    @PutMapping
    public Result<UserVO> update(@Validated @RequestBody UserSaveDTO dto) {
        return Result.ok(userService.update(dto));
    }

    @Operation(summary = "删除用户", description = "逻辑删除，并清空相关缓存")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}

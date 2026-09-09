package com.example.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rbac.common.Result;
import com.example.rbac.dto.UserPageVO;
import com.example.rbac.dto.UserSaveDTO;
import com.example.rbac.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口：每个方法都标注了权限点，而不是只停留在「登录校验」
 * （对应 docs/《Controller 加了 [Authorize] 就万事大吉？》的越权隐患分析）
 */
@Tag(name = "用户管理接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户分页查询")
    @SaCheckPermission("user:list")
    @GetMapping("/page")
    public Result<Page<UserPageVO>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String username) {
        return Result.ok(userService.page(current, size, username));
    }

    @Operation(summary = "新增用户")
    @SaCheckPermission("user:add")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody UserSaveDTO dto) {
        dto.setId(null);
        userService.save(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑用户")
    @SaCheckPermission("user:edit")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody UserSaveDTO dto) {
        userService.save(dto);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @SaCheckPermission("user:remove")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        userService.remove(id);
        return Result.ok();
    }
}

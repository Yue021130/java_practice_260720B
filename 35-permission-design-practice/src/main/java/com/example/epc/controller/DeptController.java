package com.example.epc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.epc.common.Result;
import com.example.epc.dto.DeptSaveDTO;
import com.example.epc.dto.DeptVO;
import com.example.epc.service.DeptService;
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
 * 部门管理接口。
 */
@Tag(name = "部门管理", description = "部门查询与 CRUD")
@RestController
@RequestMapping("/api/dept")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @SaCheckPermission("dept:list")
    @Operation(summary = "部门列表", description = "返回全部部门平铺列表，前端自行建树")
    @GetMapping("/tree")
    public Result<List<DeptVO>> tree() {
        return Result.ok(deptService.list());
    }

    @SaCheckPermission("dept:create")
    @Operation(summary = "新增部门")
    @PostMapping
    public Result<Void> save(@Validated @RequestBody DeptSaveDTO dto) {
        deptService.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("dept:update")
    @Operation(summary = "更新部门")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody DeptSaveDTO dto) {
        deptService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("dept:delete")
    @Operation(summary = "删除部门", description = "存在子部门时不允许删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}

package com.example.vmp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.vmp.common.Result;
import com.example.vmp.dto.CustomerSaveDTO;
import com.example.vmp.dto.CustomerVO;
import com.example.vmp.service.CustomerService;
import com.example.vmp.validation.groups.ValidationGroups;
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
 * 客户管理接口：CRUD，出参已脱敏。
 */
@Tag(name = "客户管理", description = "客户 CRUD（出参脱敏）")
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "分页列表")
    @GetMapping("/page")
    public Result<Page<CustomerVO>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) String phone) {
        return Result.ok(customerService.page(current, size, name, phone));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    public Result<CustomerVO> detail(@PathVariable Long id) {
        return Result.ok(customerService.getById(id));
    }

    @Operation(summary = "新增客户")
    @PostMapping
    public Result<CustomerVO> save(@Validated(ValidationGroups.Create.class) @RequestBody CustomerSaveDTO dto) {
        return Result.ok(customerService.save(dto));
    }

    @Operation(summary = "编辑客户", description = "敏感字段留空表示不修改")
    @PutMapping
    public Result<CustomerVO> update(@Validated(ValidationGroups.Update.class) @RequestBody CustomerSaveDTO dto) {
        return Result.ok(customerService.update(dto));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }
}

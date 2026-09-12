package com.example.sfp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sfp.common.Result;
import com.example.sfp.dto.ResourceSaveDTO;
import com.example.sfp.dto.ResourceVO;
import com.example.sfp.service.ResourceService;
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
 * 资源管理接口：CRUD，给拦截器提供真实业务流量。
 */
@Tag(name = "资源管理", description = "资源 CRUD")
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(summary = "分页列表")
    @GetMapping("/page")
    public Result<Page<ResourceVO>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) Integer type) {
        return Result.ok(resourceService.page(current, size, name, type));
    }

    @Operation(summary = "资源详情")
    @GetMapping("/{id}")
    public Result<ResourceVO> detail(@PathVariable Long id) {
        return Result.ok(resourceService.getById(id));
    }

    @Operation(summary = "新增资源")
    @PostMapping
    public Result<ResourceVO> save(@Validated @RequestBody ResourceSaveDTO dto) {
        return Result.ok(resourceService.save(dto));
    }

    @Operation(summary = "编辑资源")
    @PutMapping
    public Result<ResourceVO> update(@Validated @RequestBody ResourceSaveDTO dto) {
        return Result.ok(resourceService.update(dto));
    }

    @Operation(summary = "限流测试", description = "连续调用可观察 429 限流效果")
    @GetMapping("/test-rate-limit")
    public Result<String> testRateLimit() {
        return Result.ok("请求通过");
    }

    @Operation(summary = "删除资源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.ok();
    }
}

package com.example.mp.service;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * IService / ServiceImpl 基础 CRUD 演示。
 */
@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
@Tag(name = "IService CRUD", description = "save / saveOrUpdate / list / page")
public class ServiceCrudController {

    private final ServiceCrudService serviceCrudService;

    @PostMapping("/save")
    @Operation(summary = "save 保存", description = "使用 IService.save 保存单条")
    public ApiResponse<Map<String, Object>> save() {
        return ApiResponse.success(serviceCrudService.saveDemo());
    }

    @PostMapping("/save-or-update")
    @Operation(summary = "saveOrUpdate 保存或更新", description = "根据主键是否存在决定 insert 还是 update")
    public ApiResponse<Map<String, Object>> saveOrUpdate() {
        return ApiResponse.success(serviceCrudService.saveOrUpdateDemo());
    }

    @PostMapping("/list")
    @Operation(summary = "list 查询", description = "使用 IService.list 查询全部有效用户")
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(serviceCrudService.listDemo());
    }

    @PostMapping("/page")
    @Operation(summary = "page 分页", description = "使用 IService.page 分页查询")
    public ApiResponse<Map<String, Object>> page() {
        return ApiResponse.success(serviceCrudService.pageDemo());
    }
}

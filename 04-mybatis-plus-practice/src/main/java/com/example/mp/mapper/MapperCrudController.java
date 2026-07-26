package com.example.mp.mapper;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * BaseMapper 基础 CRUD 演示。
 */
@RestController
@RequestMapping("/api/mapper")
@RequiredArgsConstructor
@Tag(name = "BaseMapper CRUD", description = "insert / selectById / updateById / deleteById")
public class MapperCrudController {

    private final MapperCrudService mapperCrudService;

    @PostMapping("/insert")
    @Operation(summary = "insert 插入", description = "使用 BaseMapper.insert 插入用户并返回主键")
    public ApiResponse<Map<String, Object>> insert() {
        return ApiResponse.success(mapperCrudService.insertDemo());
    }

    @PostMapping("/select-by-id")
    @Operation(summary = "selectById 查询", description = "根据主键查询用户")
    public ApiResponse<Map<String, Object>> selectById() {
        return ApiResponse.success(mapperCrudService.selectByIdDemo());
    }

    @PostMapping("/update-by-id")
    @Operation(summary = "updateById 更新", description = "根据主键更新用户信息")
    public ApiResponse<Map<String, Object>> updateById() {
        return ApiResponse.success(mapperCrudService.updateByIdDemo());
    }

    @PostMapping("/delete-by-id")
    @Operation(summary = "deleteById 删除", description = "根据主键删除用户（物理删除）")
    public ApiResponse<Map<String, Object>> deleteById() {
        return ApiResponse.success(mapperCrudService.deleteByIdDemo());
    }
}

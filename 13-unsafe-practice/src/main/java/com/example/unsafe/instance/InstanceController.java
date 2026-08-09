package com.example.unsafe.instance;

import com.example.unsafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 03. 绕过构造器实验接口。
 */
@RestController
@RequestMapping("/api/instance")
@RequiredArgsConstructor
@Tag(name = "03. 绕过构造器", description = "allocateInstance / new 对比 / 真实用途与风险")
public class InstanceController {

    private final InstanceService service;

    @GetMapping("/create")
    @Operation(summary = "allocateInstance 演示", description = "不调用构造器创建对象，对比 new 的差异（含校验被绕过）")
    public ApiResponse<Map<String, Object>> create() {
        return ApiResponse.success(service.create());
    }

    @GetMapping("/compare")
    @Operation(summary = "new vs allocateInstance 对比表", description = "构造器/字段/final/校验/速度/用途逐项对比")
    public ApiResponse<Map<String, Object>> compare() {
        return ApiResponse.success(service.compare());
    }

    @GetMapping("/uses")
    @Operation(summary = "真实用途与风险", description = "Kryo / 深拷贝 / 单例破解 / 反序列化攻击面")
    public ApiResponse<Map<String, Object>> uses() {
        return ApiResponse.success(service.uses());
    }
}

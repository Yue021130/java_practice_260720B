package com.example.unsafe.memory;

import com.example.unsafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 02. 堆外内存实验接口。
 */
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
@Tag(name = "02. 堆外内存", description = "allocateMemory / put-get / setMemory / copyMemory / 泄漏风险")
public class MemoryController {

    private final MemoryService service;

    @PostMapping("/allocate")
    @Operation(summary = "堆外内存分配与读写", description = "allocateMemory 分配 → putInt/getInt 按偏移读写 → freeMemory 释放")
    public ApiResponse<Map<String, Object>> allocate(@RequestParam(defaultValue = "5") int count) {
        return ApiResponse.success(service.allocate(count));
    }

    @GetMapping("/setcopy")
    @Operation(summary = "setMemory 填充 + copyMemory 拷贝", description = "批量填 0x5A 再整体拷贝，十六进制验证")
    public ApiResponse<Map<String, Object>> setCopy() {
        return ApiResponse.success(service.setCopy());
    }

    @PostMapping("/leak")
    @Operation(summary = "堆外内存泄漏风险演示", description = "分配 N 块 1MB 堆外内存，观察堆几乎不变，结束后手动释放")
    public ApiResponse<Map<String, Object>> leak(@RequestParam(defaultValue = "3") int blocks) {
        return ApiResponse.success(service.leakDemo(blocks));
    }
}

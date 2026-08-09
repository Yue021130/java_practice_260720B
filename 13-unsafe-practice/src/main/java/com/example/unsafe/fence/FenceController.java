package com.example.unsafe.fence;

import com.example.unsafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 07. 内存屏障实验接口。
 */
@RestController
@RequestMapping("/api/fence")
@RequiredArgsConstructor
@Tag(name = "07. 内存屏障", description = "loadFence / storeFence / fullFence / JMM 与 volatile")
public class FenceController {

    private final FenceService service;

    @GetMapping("/demo")
    @Operation(summary = "内存屏障现场演示", description = "普通字段 + 屏障手写 volatile 效果，三种屏障类型说明")
    public ApiResponse<Map<String, Object>> demo() {
        try {
            return ApiResponse.success(service.demo());
        } catch (NoSuchFieldException e) {
            return ApiResponse.error(400, "字段不存在：" + e.getMessage());
        }
    }

    @GetMapping("/explain")
    @Operation(summary = "JMM 与 volatile 底层", description = "两层语义 / 8 种内存操作 / 4 条 Happens-Before 规则 / x86 实现")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

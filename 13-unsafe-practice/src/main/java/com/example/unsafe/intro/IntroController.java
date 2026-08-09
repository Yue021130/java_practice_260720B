package com.example.unsafe.intro;

import com.example.unsafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 01. 初识 Unsafe 实验接口。
 */
@RestController
@RequestMapping("/api/intro")
@RequiredArgsConstructor
@Tag(name = "01. 初识 Unsafe", description = "获取实例 / 能力地图 / getUnsafe 限制 / 为什么叫魔法类")
public class IntroController {

    private final IntroService service;

    @GetMapping("/info")
    @Operation(summary = "Unsafe 实例与能力地图", description = "验证反射拿到的 Unsafe 可用，并输出六大能力分区")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.success(service.info());
    }

    @GetMapping("/getunsafe-demo")
    @Operation(summary = "getUnsafe() 正规入口演示", description = "普通应用直接调用必抛 SecurityException，看它为什么被堵死")
    public ApiResponse<Map<String, Object>> getUnsafeDemo() {
        return ApiResponse.success(service.getUnsafeDemo());
    }

    @GetMapping("/why")
    @Operation(summary = "为什么叫魔法类 / 为什么禁用", description = "魔法类名字的由来与官方不推荐使用的四类原因")
    public ApiResponse<Map<String, Object>> why() {
        return ApiResponse.success(service.why());
    }
}

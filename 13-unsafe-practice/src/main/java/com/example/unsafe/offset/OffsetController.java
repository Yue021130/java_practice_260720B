package com.example.unsafe.offset;

import com.example.unsafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 05. 字段偏移与对象布局实验接口。
 */
@RestController
@RequestMapping("/api/offset")
@RequiredArgsConstructor
@Tag(name = "05. 字段偏移与对象布局", description = "objectFieldOffset / 打破封装 / 数组定位 / 布局示意")
public class OffsetController {

    private final OffsetService service;

    @GetMapping("/fields")
    @Operation(summary = "字段偏移量一览", description = "打印 LayoutDemo 各字段的 objectFieldOffset，推断对象头大小")
    public ApiResponse<Map<String, Object>> fields() {
        return ApiResponse.success(service.fields());
    }

    @GetMapping("/directwrite")
    @Operation(summary = "用偏移量打破封装", description = "绕过 getter/setter 直接读写 private 字段")
    public ApiResponse<Map<String, Object>> directWrite() {
        try {
            return ApiResponse.success(service.directWrite());
        } catch (NoSuchFieldException e) {
            return ApiResponse.error(400, "字段不存在：" + e.getMessage());
        }
    }

    @GetMapping("/array")
    @Operation(summary = "数组元素定位", description = "arrayBaseOffset + arrayIndexScale 直接寻址读取数组")
    public ApiResponse<Map<String, Object>> array() {
        return ApiResponse.success(service.array());
    }

    @GetMapping("/layout")
    @Operation(summary = "对象内存布局示意图", description = "对象头 / 字段区 / 对齐填充，与锁、缓存行、压缩指针的关系")
    public ApiResponse<Map<String, Object>> layout() {
        return ApiResponse.success(service.layout());
    }
}

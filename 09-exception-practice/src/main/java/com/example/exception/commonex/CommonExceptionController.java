package com.example.exception.commonex;

import com.example.exception.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 常见异常场景实验接口。
 */
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
@Tag(name = "03. 常见异常场景", description = "NPE、CCE、NumberFormat、IndexOutOfBounds、CME、UOE、StackOverflow、OOM 等")
public class CommonExceptionController {

    private final CommonExceptionScenarioService service;

    @PostMapping("/npe")
    @Operation(summary = "NPE 防御", description = "自动拆箱、链式调用、Map.get、equals 反写")
    public ApiResponse<Map<String, Object>> npe() {
        return ApiResponse.success(service.npeDefense());
    }

    @PostMapping("/class-cast")
    @Operation(summary = "ClassCastException", description = "泛型擦除与 instanceof 防御")
    public ApiResponse<Map<String, Object>> classCast() {
        return ApiResponse.success(service.classCast());
    }

    @PostMapping("/number-format")
    @Operation(summary = "NumberFormatException", description = "parse 与 BigDecimal 字符串构造")
    public ApiResponse<Map<String, Object>> numberFormat() {
        return ApiResponse.success(service.numberFormat());
    }

    @PostMapping("/index-out-of-bounds")
    @Operation(summary = "IndexOutOfBoundsException", description = "数组、List、String 越界")
    public ApiResponse<Map<String, Object>> indexOutOfBounds() {
        return ApiResponse.success(service.indexOutOfBounds());
    }

    @PostMapping("/cme")
    @Operation(summary = "ConcurrentModificationException", description = "fail-fast 与正确删除元素")
    public ApiResponse<Map<String, Object>> cme() {
        return ApiResponse.success(service.concurrentModification());
    }

    @PostMapping("/uoe")
    @Operation(summary = "UnsupportedOperationException", description = "Arrays.asList、singletonList、unmodifiableList")
    public ApiResponse<Map<String, Object>> uoe() {
        return ApiResponse.success(service.unsupportedOperation());
    }

    @PostMapping("/no-such-element")
    @Operation(summary = "NoSuchElementException", description = "Optional.get / Iterator.next")
    public ApiResponse<Map<String, Object>> noSuchElement() {
        return ApiResponse.success(service.noSuchElement());
    }

    @PostMapping("/stack-overflow")
    @Operation(summary = "StackOverflowError", description = "受控演示递归导致的栈溢出")
    public ApiResponse<Map<String, Object>> stackOverflow() {
        return ApiResponse.success(service.stackOverflow());
    }

    @GetMapping("/oom")
    @Operation(summary = "OutOfMemoryError", description = "堆 OOM、元空间 OOM、堆外内存 OOM 原理与示例")
    public ApiResponse<Map<String, Object>> oom() {
        return ApiResponse.success(service.oom());
    }

    @PostMapping("/class-not-found")
    @Operation(summary = "ClassNotFoundException vs NoClassDefFoundError", description = "编译期缺失 vs 运行期缺失")
    public ApiResponse<Map<String, Object>> classNotFound() {
        return ApiResponse.success(service.classNotFound());
    }

    @GetMapping("/assertion")
    @Operation(summary = "AssertionError", description = "assert 关键字使用与注意")
    public ApiResponse<Map<String, Object>> assertion() {
        return ApiResponse.success(service.assertion());
    }
}

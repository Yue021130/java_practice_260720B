package com.example.caa.builtin;

import com.example.caa.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置注解演示接口。
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Tag(name = "内置注解演示", description = "@Override / @Deprecated / @SuppressWarnings")
public class BuiltinAnnotationController {

    private final BuiltinAnnotationDemo builtinAnnotationDemo;

    @GetMapping("/builtin")
    @Operation(summary = "内置注解示例", description = "演示 @Override / @Deprecated / @SuppressWarnings 的用法")
    public ApiResponse<Map<String, Object>> builtin() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("override", builtinAnnotationDemo.toString());
        result.put("deprecated", builtinAnnotationDemo.oldMethod());
        result.put("suppressWarnings", builtinAnnotationDemo.suppressWarningDemo());
        result.put("tips", new String[]{
                "@Override：覆盖父类/接口方法时加上，防止笔误",
                "@Deprecated：标记过时方法，配合 javadoc @deprecated 说明替代方案",
                "@SuppressWarnings：抑制已知警告，范围越小越好"
        });
        return ApiResponse.ok(result);
    }
}

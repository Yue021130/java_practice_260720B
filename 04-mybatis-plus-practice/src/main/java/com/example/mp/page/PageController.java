package com.example.mp.page;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 分页查询演示。
 */
@RestController
@RequestMapping("/api/page")
@RequiredArgsConstructor
@Tag(name = "分页查询", description = "Page<T> + 分页插件 + Wrapper 组合")
public class PageController {

    private final PageService pageService;

    @PostMapping("/basic")
    @Operation(summary = "基础分页", description = "使用 Page<T> 与 BaseMapper.selectPage")
    public ApiResponse<Map<String, Object>> basic() {
        return ApiResponse.success(pageService.basicDemo());
    }

    @PostMapping("/custom")
    @Operation(summary = "分页 + 条件", description = "Page<T> + QueryWrapper 组合分页查询")
    public ApiResponse<Map<String, Object>> custom() {
        return ApiResponse.success(pageService.customDemo());
    }
}

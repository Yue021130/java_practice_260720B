package com.example.mp.realworld;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 综合实战演示：关联查询、分组统计、综合搜索分页。
 */
@RestController
@RequestMapping("/api/realworld")
@RequiredArgsConstructor
@Tag(name = "综合实战", description = "用户订单 / 分组统计 / 搜索分页")
public class RealWorldController {

    private final RealWorldService realWorldService;

    @PostMapping("/user-order")
    @Operation(summary = "用户订单统计", description = "自定义 SQL 查询每个用户的订单金额与数量")
    public ApiResponse<Map<String, Object>> userOrder() {
        return ApiResponse.success(realWorldService.userOrderStats());
    }

    @PostMapping("/status-stats")
    @Operation(summary = "状态分组统计", description = "按 status 分组统计人数与平均年龄")
    public ApiResponse<Map<String, Object>> statusStats() {
        return ApiResponse.success(realWorldService.statusStats());
    }

    @PostMapping("/search-page")
    @Operation(summary = "综合搜索分页", description = "用户名模糊 + 状态等值 + 年龄范围 + 分页 + 排序")
    public ApiResponse<Map<String, Object>> searchPage() {
        return ApiResponse.success(realWorldService.searchPage());
    }
}

package com.example.os.report;

import com.example.os.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 02 订单报表统计接口。
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    @Operation(summary = "订单汇总报表", description = "按最近 N 天汇总订单数量、金额、已完成金额、平均客单价与 Top3 用户。")
    public ApiResponse<Map<String, Object>> summary(
            @Parameter(description = "最近 N 天，为空使用默认值 30", example = "30")
            @RequestParam(required = false) Integer days) {
        return ApiResponse.ok(reportService.summary(days));
    }

    @GetMapping("/by-status")
    @Operation(summary = "按状态分组", description = "按订单状态分组统计单数与金额。")
    public ApiResponse<Map<String, Object>> byStatus() {
        return ApiResponse.ok(reportService.byStatus());
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(reportService.explain());
    }
}

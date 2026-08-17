package com.example.os.dataclean;

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
 * 04 批量数据清洗接口。
 */
@RestController
@RequestMapping("/api/dataclean")
@RequiredArgsConstructor
public class DataCleanController {

    private final DataCleanService dataCleanService;

    @GetMapping("/clean")
    @Operation(summary = "批量数据清洗", description = "清洗原始脏数据，演示 Optional 单字段处理 + Stream 批量过滤。")
    public ApiResponse<Map<String, Object>> clean(
            @Parameter(description = "最大处理条数，默认使用配置值 1000", example = "100")
            @RequestParam(required = false) Integer maxRows) {
        return ApiResponse.ok(dataCleanService.clean(maxRows));
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(dataCleanService.explain());
    }
}

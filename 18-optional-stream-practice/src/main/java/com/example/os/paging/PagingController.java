package com.example.os.paging;

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
 * 08 分页结果再加工接口。
 */
@RestController
@RequestMapping("/api/paging")
@RequiredArgsConstructor
public class PagingController {

    private final PagingService pagingService;

    @GetMapping("/transform")
    @Operation(summary = "分页结果再加工", description = "模拟分页查询后，对 records 做过滤、排序、字段转换。")
    public ApiResponse<Map<String, Object>> transform(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小", example = "5")
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(pagingService.transform(page, size));
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(pagingService.explain());
    }
}

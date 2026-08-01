package com.example.tl.advance;

import com.example.tl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/advance")
@RequiredArgsConstructor
@Tag(name = "进阶", description = "内存泄漏原理 / 最佳实践")
public class AdvanceController {

    private final AdvanceService advanceService;

    @PostMapping("/leak-analysis")
    @Operation(summary = "内存泄漏原理", description = "ThreadLocalMap 的 key 是弱引用、value 是强引用")
    public ApiResponse<Map<String, Object>> leakAnalysis() {
        return ApiResponse.success(advanceService.leakAnalysisDemo());
    }

    @PostMapping("/best-practice")
    @Operation(summary = "最佳实践", description = "static final + try-finally remove + 线程池注意事项")
    public ApiResponse<Map<String, Object>> bestPractice() {
        return ApiResponse.success(advanceService.bestPracticeDemo());
    }
}

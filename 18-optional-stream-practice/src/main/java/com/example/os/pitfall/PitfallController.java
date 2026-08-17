package com.example.os.pitfall;

import com.example.os.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 09 反模式对比接口。
 */
@RestController
@RequestMapping("/api/pitfall")
@RequiredArgsConstructor
public class PitfallController {

    private final PitfallService pitfallService;

    @GetMapping("/wrong-vs-right")
    @Operation(summary = "错误 vs 正确写法", description = "对比 Optional + Stream 的 4 组常见反模式与推荐写法。")
    public ApiResponse<Map<String, Object>> wrongVsRight() {
        return ApiResponse.ok(pitfallService.wrongVsRight());
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回反模式清单与最佳实践。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(pitfallService.explain());
    }
}

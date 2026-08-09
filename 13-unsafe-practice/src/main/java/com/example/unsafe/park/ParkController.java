package com.example.unsafe.park;

import com.example.unsafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 06. 线程阻塞与唤醒实验接口。
 */
@RestController
@RequestMapping("/api/park")
@RequiredArgsConstructor
@Tag(name = "06. park/unpark", description = "阻塞唤醒演示 / 与 wait-notify 对比 / LockSupport 原理")
public class ParkController {

    private final ParkService service;

    @GetMapping("/demo")
    @Operation(summary = "park/unpark 现场演示", description = "正常顺序唤醒 + 提前 unpark 许可证机制，输出完整时间线")
    public ApiResponse<Map<String, Object>> demo() {
        return ApiResponse.success(service.demo());
    }

    @GetMapping("/compare")
    @Operation(summary = "park vs wait/notify 对比表", description = "锁要求 / 顺序 / 精确唤醒 / 超时 / 中断")
    public ApiResponse<Map<String, Object>> compare() {
        return ApiResponse.success(service.compare());
    }

    @GetMapping("/explain")
    @Operation(summary = "LockSupport 原理", description = "许可证机制 / AQS 里怎么用 / 为什么不用 wait")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

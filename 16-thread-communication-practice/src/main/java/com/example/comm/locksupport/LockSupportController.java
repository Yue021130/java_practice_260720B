package com.example.comm.locksupport;

import com.example.comm.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 05. 基于线程协作控制：LockSupport 的 park / unpark。
 */
@RestController
@RequestMapping("/api/locksupport")
@RequiredArgsConstructor
@Tag(name = "05. LockSupport", description = "park/unpark / 信号可预发 / AQS 基石")
public class LockSupportController {

    private final LockSupportService service;

    @GetMapping("/park-unpark")
    @Operation(summary = "park 后 unpark 唤醒", description = "线程 park 阻塞，主线程延迟 unpark，记录等待耗时")
    public ApiResponse<Map<String, Object>> parkUnpark(@RequestParam(defaultValue = "200") int delayMs) {
        return ApiResponse.success(service.parkUnpark(delayMs));
    }

    @GetMapping("/unpark-first")
    @Operation(summary = "先 unpark 后 park（信号预发）", description = "许可提前发，park 立刻通过；wait 这么做会死锁")
    public ApiResponse<Map<String, Object>> unparkFirst() {
        return ApiResponse.success(service.unparkFirst());
    }

    @GetMapping("/explain")
    @Operation(summary = "LockSupport 速记（八股）", description = "与 wait/notify 区别 / permit 机制 / AQS 基石")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

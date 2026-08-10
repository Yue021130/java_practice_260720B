package com.example.comm.condition;

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
 * 03. 基于锁对象 / 等待通知：Condition（Lock 的条件队列）。
 */
@RestController
@RequestMapping("/api/condition")
@RequiredArgsConstructor
@Tag(name = "03. Condition 条件队列", description = "一个锁挂多个等待队列 / 精准唤醒 / 有界缓冲")
public class ConditionController {

    private final ConditionService service;

    @GetMapping("/bounded-buffer")
    @Operation(summary = "Condition 有界缓冲", description = "notFull / notEmpty 两个条件队列，比 wait/notify 更精准")
    public ApiResponse<Map<String, Object>> boundedBuffer(@RequestParam(defaultValue = "20") int productions,
                                                          @RequestParam(defaultValue = "3") int capacity) {
        return ApiResponse.success(service.boundedBuffer(productions, capacity));
    }

    @GetMapping("/signal-demo")
    @Operation(summary = "signal 精准唤醒", description = "N 个等待者分两组，signal 只唤醒其中一组（wait/notify 做不到）")
    public ApiResponse<Map<String, Object>> signalDemo(@RequestParam(defaultValue = "4") int waiters) {
        return ApiResponse.success(service.signalDemo(waiters));
    }

    @GetMapping("/explain")
    @Operation(summary = "Condition 速记（八股）", description = "与 wait/notify 区别 / signal vs signalAll / 为什么绑定 Lock")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

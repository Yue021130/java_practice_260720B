package com.example.comm.waitnotify;

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
 * 02. 基于锁对象 / 等待通知：Object 的 wait / notify / notifyAll。
 */
@RestController
@RequestMapping("/api/waitnotify")
@RequiredArgsConstructor
@Tag(name = "02. 等待通知 wait/notify", description = "monitor 等待队列 / 生产者-消费者经典模型")
public class WaitNotifyController {

    private final WaitNotifyService service;

    @GetMapping("/producer-consumer")
    @Operation(summary = "wait/notify 生产者-消费者", description = "有界缓冲，满则 wait、空则 wait，notifyAll 唤醒")
    public ApiResponse<Map<String, Object>> producerConsumer(@RequestParam(defaultValue = "20") int productions,
                                                             @RequestParam(defaultValue = "3") int capacity) {
        return ApiResponse.success(service.producerConsumer(productions, capacity));
    }

    @GetMapping("/explain")
    @Operation(summary = "wait/notify 速记（八股）", description = "为什么必须在 synchronized / 为什么 while 不用 if / notify vs notifyAll")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

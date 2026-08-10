package com.example.comm.queue;

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
 * 07. 基于阻塞队列：BlockingQueue 生产者-消费者标准解。
 */
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Tag(name = "07. 阻塞队列 BlockingQueue", description = "put/take 阻塞 / 背压 / 队列家族选型")
public class QueueController {

    private final QueueService service;

    @GetMapping("/blocking-demo")
    @Operation(summary = "put/take 阻塞演示", description = "满则阻塞生产者、空则阻塞消费者（背压）")
    public ApiResponse<Map<String, Object>> blockingDemo(@RequestParam(defaultValue = "20") int productions,
                                                         @RequestParam(defaultValue = "3") int capacity) {
        return ApiResponse.success(service.blockingDemo(productions, capacity));
    }

    @GetMapping("/family")
    @Operation(summary = "阻塞队列家族速览", description = "Array / Linked / Synchronous / Priority / Delay 各显神通")
    public ApiResponse<Map<String, Object>> family() {
        return ApiResponse.success(service.family());
    }

    @GetMapping("/explain")
    @Operation(summary = "阻塞队列速记（八股）", description = "原理(Condition) / put-take vs offer-poll / 背压 / 选型")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

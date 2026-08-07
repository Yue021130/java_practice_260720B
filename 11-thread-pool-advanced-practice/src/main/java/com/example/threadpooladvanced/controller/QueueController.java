package com.example.threadpooladvanced.controller;

import com.example.threadpooladvanced.common.ApiResponse;
import com.example.threadpooladvanced.dto.QueueExperimentResult;
import com.example.threadpooladvanced.dto.QueueTypeDto;
import com.example.threadpooladvanced.service.ThreadPoolExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 阻塞队列类型说明与实验。
 */
@Tag(name = "02. 阻塞队列", description = "7 种 BlockingQueue 特性对比与独立实验")
@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private ThreadPoolExperimentService experimentService;

    @Operation(summary = "7 种阻塞队列说明")
    @GetMapping("/types")
    public ApiResponse<List<QueueTypeDto>> types() {
        return ApiResponse.ok(experimentService.listQueueTypes());
    }

    @Operation(summary = "指定队列类型实验")
    @PostMapping("/{type}/experiment")
    public ApiResponse<QueueExperimentResult> experiment(
            @PathVariable String type,
            @RequestParam(defaultValue = "5") int capacity,
            @RequestParam(defaultValue = "10") int submitCount) {
        return ApiResponse.ok(experimentService.experimentQueue(type, capacity, submitCount));
    }
}

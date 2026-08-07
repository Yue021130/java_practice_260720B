package com.example.threadpooladvanced.controller;

import com.example.threadpooladvanced.common.ApiResponse;
import com.example.threadpooladvanced.dto.ExecutorsTypeDto;
import com.example.threadpooladvanced.service.ThreadPoolExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Executors 工厂方法说明与风险演示。
 */
@Tag(name = "04. Executors 工厂", description = "4 种工厂方法源码、特点、风险与演示")
@RestController
@RequestMapping("/api/executors")
public class ExecutorsController {

    @Autowired
    private ThreadPoolExperimentService experimentService;

    @Operation(summary = "Executors 4 种工厂说明")
    @GetMapping("/types")
    public ApiResponse<List<ExecutorsTypeDto>> types() {
        return ApiResponse.ok(experimentService.listExecutorsTypes());
    }

    @Operation(summary = "指定工厂类型演示")
    @PostMapping("/{type}/demo")
    public ApiResponse<Map<String, Object>> demo(@PathVariable String type) {
        return ApiResponse.ok(experimentService.demoExecutors(type));
    }
}

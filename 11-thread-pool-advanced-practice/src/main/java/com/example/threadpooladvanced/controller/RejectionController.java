package com.example.threadpooladvanced.controller;

import com.example.threadpooladvanced.common.ApiResponse;
import com.example.threadpooladvanced.dto.RejectionExperimentResult;
import com.example.threadpooladvanced.dto.RejectionTypeDto;
import com.example.threadpooladvanced.service.ThreadPoolExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 拒绝策略说明与实验。
 */
@Tag(name = "03. 拒绝策略", description = "4 种 JDK 拒绝策略 + 自定义策略说明与实验")
@RestController
@RequestMapping("/api/rejection")
public class RejectionController {

    @Autowired
    private ThreadPoolExperimentService experimentService;

    @Operation(summary = "拒绝策略说明")
    @GetMapping("/types")
    public ApiResponse<List<RejectionTypeDto>> types() {
        return ApiResponse.ok(experimentService.listRejectionTypes());
    }

    @Operation(summary = "指定拒绝策略实验")
    @PostMapping("/{policy}/experiment")
    public ApiResponse<RejectionExperimentResult> experiment(
            @PathVariable String policy,
            @RequestParam(defaultValue = "10") int submitCount) {
        return ApiResponse.ok(experimentService.experimentRejection(policy, submitCount));
    }
}

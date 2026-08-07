package com.example.threadpooladvanced.controller;

import com.example.threadpooladvanced.common.ApiResponse;
import com.example.threadpooladvanced.dto.*;
import com.example.threadpooladvanced.service.ThreadPoolExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ThreadPoolExecutor 源码流程与生命周期说明。
 */
@Tag(name = "05. 源码分析", description = "execute() 流程、生命周期状态、Worker 内部类")
@RestController
@RequestMapping("/api/source")
public class SourceController {

    @Autowired
    private ThreadPoolExperimentService experimentService;

    @Operation(summary = "execute() 执行流程")
    @GetMapping("/execute-flow")
    public ApiResponse<List<SourceFlowStepDto>> executeFlow() {
        return ApiResponse.ok(experimentService.getExecuteFlow());
    }

    @Operation(summary = "线程池生命周期状态")
    @GetMapping("/lifecycle-states")
    public ApiResponse<List<LifecycleStateDto>> lifecycleStates() {
        return ApiResponse.ok(experimentService.getLifecycleStates());
    }

    @Operation(summary = "Worker 内部类说明")
    @GetMapping("/worker-intro")
    public ApiResponse<List<WorkerIntroDto>> workerIntro() {
        return ApiResponse.ok(experimentService.getWorkerIntro());
    }
}

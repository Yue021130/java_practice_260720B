package com.example.comm.cooperate;

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
 * 04. 基于线程协作控制：Thread.join / interrupt 中断。
 */
@RestController
@RequestMapping("/api/cooperate")
@RequiredArgsConstructor
@Tag(name = "04. 线程协作控制", description = "join 等待结束 / interrupt 优雅退出")
public class CooperateController {

    private final CooperateService service;

    @GetMapping("/join-demo")
    @Operation(summary = "join 等待演示", description = "主线程等 N 个子任务全部完成后才继续")
    public ApiResponse<Map<String, Object>> joinDemo(@RequestParam(defaultValue = "3") int tasks,
                                                     @RequestParam(defaultValue = "100") int taskMs) {
        return ApiResponse.success(service.joinDemo(tasks, taskMs));
    }

    @GetMapping("/interrupt-demo")
    @Operation(summary = "interrupt 优雅退出演示", description = "sleep 被打断抛异常 / 循环里 isInterrupted 感知退出")
    public ApiResponse<Map<String, Object>> interruptDemo(@RequestParam(defaultValue = "sleep") String mode) {
        return ApiResponse.success(service.interruptDemo(mode));
    }

    @GetMapping("/explain")
    @Operation(summary = "join / interrupt 速记（八股）", description = "join 底层 / interrupt 是协作不是强杀 / 中断状态清理")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

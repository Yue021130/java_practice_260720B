package com.example.cg.queue;

import com.example.cg.common.ApiResponse;
import com.example.cg.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Queue / Deque 场景 REST 接口。
 *
 * 覆盖 PriorityQueue TopK 与 ArrayDeque 双端队列。
 */
@Tag(name = "Queue 场景", description = "PriorityQueue / ArrayDeque")
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueScenarioService queueScenarioService;

    @Operation(summary = "PriorityQueue TopK", description = "从海量词频中用最小堆取 TopK")
    @PostMapping("/priorityqueue-topk")
    public ApiResponse<ScenarioResult> priorityqueueTopk(
            @Parameter(description = "取前 K 个", example = "10") @RequestParam(defaultValue = "10") int k) {
        if (k < 1 || k > 50) {
            return ApiResponse.error(400, "k 必须在 1~50 之间");
        }
        return ApiResponse.success(queueScenarioService.priorityqueueTopk(k));
    }

    @Operation(summary = "ArrayDeque 双端队列", description = "同时演示栈（push/pop）与队列（offer/poll）用法")
    @PostMapping("/arraydeque-dual")
    public ApiResponse<ScenarioResult> arraydequeDual() {
        return ApiResponse.success(queueScenarioService.arraydequeDual());
    }
}

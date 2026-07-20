package com.example.threadpool.controller;

import com.example.threadpool.dto.ApiResponse;
import com.example.threadpool.dto.PoolMetricsDto;
import com.example.threadpool.service.PoolMetricsService;
import com.example.threadpool.service.TaskSubmitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线程池实验 REST 接口。
 *
 * 三个接口分别对应：提交压测任务 / 查看实时指标 / 动态调整参数。
 * 入参在 Controller 层做校验，非法参数返回 400 + 中文提示。
 */
@Tag(name = "线程池管理", description = "提交压测任务 / 查看实时指标 / 动态调整参数")
@RestController
@RequestMapping("/api/pool")
@RequiredArgsConstructor
public class ThreadPoolController {

    private final TaskSubmitService taskSubmitService;
    private final PoolMetricsService poolMetricsService;

    /**
     * 提交 N 个模拟任务到指定线程池。
     *
     * 示例：POST /api/pool/customPool/submit?count=20&taskDurationMs=2000
     * 向故意配小的 customPool 灌 20 个耗时 2 秒的任务，观察拒绝数上涨。
     */
    @Operation(summary = "提交压测任务", description = "向指定线程池提交 N 个模拟任务（sleep 模拟耗时），返回成功入池数与被拒绝数")
    @PostMapping("/{poolName}/submit")
    public ApiResponse<Map<String, Object>> submit(
            @Parameter(description = "池名：cpuPool / ioPool / customPool", example = "customPool")
            @PathVariable String poolName,
            @Parameter(description = "提交任务数，1~1000", example = "20")
            @RequestParam(defaultValue = "10") int count,
            @Parameter(description = "单任务模拟耗时（毫秒），0~60000", example = "2000")
            @RequestParam(defaultValue = "500") long taskDurationMs) {
        if (count < 1 || count > 1000) {
            return ApiResponse.error(400, "count 必须在 1~1000 之间");
        }
        if (taskDurationMs < 0 || taskDurationMs > 60_000) {
            return ApiResponse.error(400, "taskDurationMs 必须在 0~60000 之间");
        }
        try {
            int submitted = taskSubmitService.submitTasks(poolName, count, taskDurationMs);
            Map<String, Object> data = new HashMap<>();
            data.put("requested", count);
            // submitted < requested 说明部分任务被 AbortPolicy 拒绝
            data.put("submitted", submitted);
            data.put("rejected", count - submitted);
            return ApiResponse.success("提交完成", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取所有线程池的实时监控指标。
     *
     * 示例：GET /api/pool/metrics
     * 前端每 2 秒轮询一次本接口刷新面板。
     */
    @Operation(summary = "查看实时指标", description = "返回所有线程池的活跃线程、池大小、队列积压、已完成任务数、被拒绝任务数等指标")
    @GetMapping("/metrics")
    public ApiResponse<List<PoolMetricsDto>> metrics() {
        return ApiResponse.success(poolMetricsService.getAllMetrics());
    }

    /**
     * 动态调整指定线程池的核心/最大线程数。
     *
     * 示例：POST /api/pool/ioPool/resize?corePoolSize=32&maxPoolSize=64
     * 校验规则：corePoolSize >= 1，maxPoolSize >= corePoolSize。
     */
    @Operation(summary = "动态调整参数", description = "运行期修改指定线程池的核心/最大线程数，校验 corePoolSize >= 1 且 maxPoolSize >= corePoolSize")
    @PostMapping("/{poolName}/resize")
    public ApiResponse<Map<String, Object>> resize(
            @Parameter(description = "池名：cpuPool / ioPool / customPool", example = "ioPool")
            @PathVariable String poolName,
            @Parameter(description = "新的核心线程数，>= 1", example = "32")
            @RequestParam int corePoolSize,
            @Parameter(description = "新的最大线程数，>= corePoolSize", example = "64")
            @RequestParam int maxPoolSize) {
        if (corePoolSize < 1) {
            return ApiResponse.error(400, "corePoolSize 必须 >= 1");
        }
        if (maxPoolSize < corePoolSize) {
            return ApiResponse.error(400, "maxPoolSize 必须 >= corePoolSize");
        }
        if (maxPoolSize > 500) {
            // 教学项目加个上限，防止误填超大值拖垮机器
            return ApiResponse.error(400, "maxPoolSize 不能超过 500");
        }
        try {
            poolMetricsService.resize(poolName, corePoolSize, maxPoolSize);
            Map<String, Object> data = new HashMap<>();
            data.put("poolName", poolName);
            data.put("corePoolSize", corePoolSize);
            data.put("maxPoolSize", maxPoolSize);
            return ApiResponse.success("调整成功", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}

package com.example.sl.parallel;

import com.example.sl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/parallel")
@RequiredArgsConstructor
@Tag(name = "并行流", description = "并行流加速、开销、线程安全与顺序")
public class ParallelController {

    private final ParallelService parallelService;

    @PostMapping("/speedup")
    @Operation(summary = "并行加速场景", description = "大集合 + CPU 密集型任务，parallelStream 更快")
    public ApiResponse<Map<String, Object>> speedup() {
        return ApiResponse.success(parallelService.speedupDemo());
    }

    @PostMapping("/overhead")
    @Operation(summary = "并行额外开销", description = "小集合 + 简单操作，parallelStream 更慢")
    public ApiResponse<Map<String, Object>> overhead() {
        return ApiResponse.success(parallelService.overheadDemo());
    }

    @PostMapping("/race-condition")
    @Operation(summary = "线程不安全错误示范", description = "共享可变集合 + parallelStream().forEach 导致结果错误")
    public ApiResponse<Map<String, Object>> raceCondition() {
        return ApiResponse.success(parallelService.raceConditionDemo());
    }

    @PostMapping("/correct-reduce")
    @Operation(summary = "正确聚合", description = "reduce / collect 保证结合律与无状态")
    public ApiResponse<Map<String, Object>> correctReduce() {
        return ApiResponse.success(parallelService.correctReduceDemo());
    }

    @PostMapping("/order-findany")
    @Operation(summary = "顺序与 findAny", description = "ordered / unordered、findFirst vs findAny")
    public ApiResponse<Map<String, Object>> orderFindAny() {
        return ApiResponse.success(parallelService.orderFindAnyDemo());
    }
}

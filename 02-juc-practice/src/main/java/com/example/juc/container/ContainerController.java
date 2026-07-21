package com.example.juc.container;

import com.example.juc.common.ApiResponse;
import com.example.juc.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 并发容器场景 REST 接口。
 *
 * 覆盖 ConcurrentHashMap、CopyOnWriteArrayList、DelayQueue、BlockingQueue 家族、ConcurrentSkipListMap。
 */
@Tag(name = "并发容器场景", description = "CHM / CopyOnWrite / DelayQueue / 跳表")
@RestController
@RequestMapping("/api/container")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerScenarioService containerScenarioService;

    @Operation(summary = "HashMap 并发事故", description = "多线程 put 导致 size 不准/数据丢失，CHM 做对照")
    @PostMapping("/hashmap-accident")
    public ApiResponse<ScenarioResult> hashmapAccident(
            @Parameter(description = "线程数", example = "8") @RequestParam(defaultValue = "8") int threads,
            @Parameter(description = "灌入键数", example = "10000") @RequestParam(defaultValue = "10000") int keys) {
        if (threads < 1 || threads > 32 || keys < 1000 || keys > 100000) {
            return ApiResponse.error(400, "参数越界");
        }
        return ApiResponse.success(containerScenarioService.hashmapAccident(threads, keys));
    }

    @Operation(summary = "ConcurrentHashMap 原子操作三件套", description = "putIfAbsent / computeIfAbsent / merge 三个原子复合操作")
    @PostMapping("/chm-ops")
    public ApiResponse<ScenarioResult> chmOps() {
        return ApiResponse.success(containerScenarioService.chmOps());
    }

    @Operation(summary = "CopyOnWrite 白名单", description = "读多写少场景遍历不阻塞，对比 ArrayList 的 ConcurrentModificationException")
    @PostMapping("/cow-whitelist")
    public ApiResponse<ScenarioResult> cowWhitelist() {
        return ApiResponse.success(containerScenarioService.cowWhitelist());
    }

    @Operation(summary = "DelayQueue 订单超时取消", description = "订单到期自动出队被取消")
    @PostMapping("/delayqueue-order")
    public ApiResponse<ScenarioResult> delayqueueOrder(
            @Parameter(description = "订单数", example = "5") @RequestParam(defaultValue = "5") int orders,
            @Parameter(description = "超时时间(ms)", example = "2000") @RequestParam(defaultValue = "2000") int timeoutMs) {
        if (orders < 1 || orders > 20 || timeoutMs < 500 || timeoutMs > 10000) {
            return ApiResponse.error(400, "参数越界");
        }
        return ApiResponse.success(containerScenarioService.delayqueueOrder(orders, timeoutMs));
    }

    @Operation(summary = "BlockingQueue 家族对比", description = "Array / Linked / Synchronous / Priority 四种队列特性实测")
    @PostMapping("/blockingqueue-family")
    public ApiResponse<ScenarioResult> blockingqueueFamily() {
        return ApiResponse.success(containerScenarioService.blockingqueueFamily());
    }

    @Operation(summary = "跳表排行榜", description = "多线程并发更新分数并实时取 TopN")
    @PostMapping("/skiplist-leaderboard")
    public ApiResponse<ScenarioResult> skiplistLeaderboard(
            @Parameter(description = "玩家数", example = "20") @RequestParam(defaultValue = "20") int players) {
        if (players < 5 || players > 100) {
            return ApiResponse.error(400, "players 必须在 5~100 之间");
        }
        return ApiResponse.success(containerScenarioService.skiplistLeaderboard(players));
    }
}

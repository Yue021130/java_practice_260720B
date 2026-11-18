package com.example.scp.controller;

import com.example.scp.common.Result;
import com.example.scp.dto.CacheStatsVO;
import com.example.scp.service.CacheMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 缓存监控接口：命中率统计、两级 key 分布查看、手动清空。
 *
 * <p>生产环境此类管理接口必须加权限管控（仅运维角色可见），
 * 本章为了教学演示直接放行在登录态之后。</p>
 */
@Tag(name = "缓存监控", description = "二级缓存命中率统计与 key 查看/清空")
@RestController
@RequestMapping("/api/cache")
public class CacheMonitorController {

    private final CacheMonitorService cacheMonitorService;

    public CacheMonitorController(CacheMonitorService cacheMonitorService) {
        this.cacheMonitorService = cacheMonitorService;
    }

    @Operation(summary = "缓存命中率统计", description = "返回各缓存的 L1/L2 命中、miss、驱逐次数与命中率")
    @GetMapping("/stats")
    public Result<List<CacheStatsVO>> stats() {
        return Result.ok(cacheMonitorService.stats());
    }

    @Operation(summary = "查看缓存 key 分布", description = "返回指定缓存的两级 key 列表：l1Keys（Caffeine）、l2Keys（Redis）")
    @GetMapping("/keys")
    public Result<Map<String, List<String>>> keys(
            @Parameter(description = "缓存名：product / productPage") @RequestParam(defaultValue = "product") String cache) {
        return Result.ok(cacheMonitorService.keys(cache));
    }

    @Operation(summary = "清空指定缓存", description = "两级缓存都清空（Caffeine invalidateAll + Redis scan 删除）")
    @PostMapping("/clear")
    public Result<Void> clear(
            @Parameter(description = "缓存名：product / productPage") @RequestParam(defaultValue = "product") String cache) {
        cacheMonitorService.clear(cache);
        return Result.ok();
    }
}

package com.example.ccp.controller;

import com.example.ccp.common.Result;
import com.example.ccp.service.CacheDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 缓存演示接口：直观观察 Caffeine 的命中率与「首次查库 / 再次走缓存」的差异。
 */
@Tag(name = "缓存演示", description = "Caffeine 命中率统计与缓存对比实验")
@RestController
@RequestMapping("/api/cache")
public class CacheDemoController {

    private final CacheDemoService cacheDemoService;

    public CacheDemoController(CacheDemoService cacheDemoService) {
        this.cacheDemoService = cacheDemoService;
    }

    @Operation(summary = "缓存统计", description = "查看 user / userPage 缓存的命中率、命中次数等")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(cacheDemoService.stats());
    }

    @Operation(summary = "缓存对比实验", description = "连续查询同一用户两次，第一次走库、第二次走缓存")
    @GetMapping("/compare/{id}")
    public Result<Map<String, Object>> compare(@PathVariable Long id) {
        return Result.ok(cacheDemoService.compare(id));
    }

    @Operation(summary = "清空全部缓存")
    @GetMapping("/clear")
    public Result<Void> clear() {
        cacheDemoService.clear();
        return Result.ok();
    }
}

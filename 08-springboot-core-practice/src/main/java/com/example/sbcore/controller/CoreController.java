package com.example.sbcore.controller;

import com.example.sbcore.dto.ApiResult;
import com.example.sbcore.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Spring Boot 核心能力实战", description = "12 个可运行场景")
@RestController
@RequestMapping("/api/core")
public class CoreController {

    @Autowired
    private StarterService starterService;

    @Autowired
    private AutoConfigService autoConfigService;

    @Autowired
    private ConfigPriorityService configPriorityService;

    @Autowired
    private ConfigPropsService configPropsService;

    @Autowired
    private BeanLifecycleService beanLifecycleService;

    @Autowired
    private ConditionalService conditionalService;

    @Autowired
    private CacheCaffeineService cacheCaffeineService;

    @Autowired
    private CacheOpsService cacheOpsService;

    @Autowired
    private CacheHitService cacheHitService;

    @Autowired
    private CacheRedisService cacheRedisService;

    @Autowired
    private CacheCompareService cacheCompareService;

    @Autowired
    private ActuatorService actuatorService;

    @Operation(summary = "常见 Starter 与能力清单")
    @PostMapping("/starters")
    public ApiResult<Map<String, Object>> starters() {
        return ApiResult.ok(starterService.run());
    }

    @Operation(summary = "自动装配 Bean 清单")
    @PostMapping("/auto-config-beans")
    public ApiResult<Map<String, Object>> autoConfigBeans() {
        return ApiResult.ok(autoConfigService.run());
    }

    @Operation(summary = "配置优先级与 Profile 切换")
    @PostMapping("/config-priority")
    public ApiResult<Map<String, Object>> configPriority() {
        return ApiResult.ok(configPriorityService.run());
    }

    @Operation(summary = "@ConfigurationProperties 绑定与校验")
    @PostMapping("/config-props")
    public ApiResult<Map<String, Object>> configProps() {
        return ApiResult.ok(configPropsService.run());
    }

    @Operation(summary = "Bean 生命周期回调")
    @PostMapping("/bean-lifecycle")
    public ApiResult<Map<String, Object>> beanLifecycle() {
        return ApiResult.ok(beanLifecycleService.run());
    }

    @Operation(summary = "条件装配场景")
    @PostMapping("/conditional")
    public ApiResult<Map<String, Object>> conditional() {
        return ApiResult.ok(conditionalService.run());
    }

    @Operation(summary = "@EnableCaching + Caffeine 基础缓存")
    @PostMapping("/cache-caffeine-basic")
    public ApiResult<Map<String, Object>> cacheCaffeineBasic() {
        return ApiResult.ok(cacheCaffeineService.run());
    }

    @Operation(summary = "缓存注解行为对比")
    @PostMapping("/cache-ops")
    public ApiResult<Map<String, Object>> cacheOps() {
        return ApiResult.ok(cacheOpsService.run());
    }

    @Operation(summary = "缓存命中率与耗时对比")
    @PostMapping("/cache-hit")
    public ApiResult<Map<String, Object>> cacheHit(@RequestParam(defaultValue = "50") int totalRequests) {
        return ApiResult.ok(cacheHitService.run(totalRequests));
    }

    @Operation(summary = "Redis 分布式缓存")
    @PostMapping("/cache-redis")
    public ApiResult<Map<String, Object>> cacheRedis() {
        return ApiResult.ok(cacheRedisService.run());
    }

    @Operation(summary = "本地缓存 vs 分布式缓存")
    @PostMapping("/cache-compare")
    public ApiResult<Map<String, Object>> cacheCompare() {
        return ApiResult.ok(cacheCompareService.run());
    }

    @Operation(summary = "Actuator health / info 与安全暴露")
    @PostMapping("/actuator")
    public ApiResult<Map<String, Object>> actuator() {
        return ApiResult.ok(actuatorService.run());
    }
}

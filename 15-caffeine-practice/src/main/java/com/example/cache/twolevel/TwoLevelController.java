package com.example.cache.twolevel;

import com.example.cache.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 07. 两级缓存实验接口。
 */
@RestController
@RequestMapping("/api/twolevel")
@RequiredArgsConstructor
@Tag(name = "07. 两级缓存", description = "L1 Caffeine + L2 Redis 读路径 / Cache Aside 写路径 / 一致性")
public class TwoLevelController {

    private final TwoLevelService service;

    @GetMapping("/get")
    @Operation(summary = "读路径（L1 → L2 → DB）", description = "命中哪一级从哪一级回填，返回来源")
    public ApiResponse<Map<String, Object>> get(@RequestParam(defaultValue = "1") int id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping("/put")
    @Operation(summary = "写路径（Cache Aside）", description = "先更库再删 L1+L2，下次读重新加载")
    public ApiResponse<Map<String, Object>> put(@RequestParam(defaultValue = "1") int id,
                                                @RequestParam(defaultValue = "新员工名") String name,
                                                @RequestParam(defaultValue = "新部门") String dept) {
        return ApiResponse.success(service.put(id, name, dept));
    }

    @PostMapping("/evict")
    @Operation(summary = "删两级缓存", description = "手动清 L1 + L2，强制下次读走 DB 回填")
    public ApiResponse<Map<String, Object>> evict(@RequestParam(defaultValue = "1") int id) {
        return ApiResponse.success(service.evict(id));
    }

    @GetMapping("/consistency")
    @Operation(summary = "一致性策略说明", description = "Cache Aside / 双删 / 延迟双删 / 读写穿 / 写回")
    public ApiResponse<Map<String, Object>> consistency() {
        return ApiResponse.success(service.consistency());
    }

    @GetMapping("/explain")
    @Operation(summary = "两级缓存速记（八股）", description = "为什么两级 / 各层配置 / 注意事项")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.success(service.explain());
    }
}

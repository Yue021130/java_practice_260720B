package com.example.cg.set;

import com.example.cg.common.ApiResponse;
import com.example.cg.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Set 场景 REST 接口。
 *
 * 覆盖 HashSet / LinkedHashSet / TreeSet 与 equals/hashCode 契约。
 */
@Tag(name = "Set 场景", description = "HashSet / LinkedHashSet / TreeSet / equals-hashCode")
@RestController
@RequestMapping("/api/set")
@RequiredArgsConstructor
public class SetController {

    private final SetScenarioService setScenarioService;

    @Operation(summary = "HashSet 去重", description = "自定义对象未重写 equals/hashCode 时无法去重")
    @PostMapping("/hashset-dedup")
    public ApiResponse<ScenarioResult> hashsetDedup() {
        return ApiResponse.success(setScenarioService.hashsetDedup());
    }

    @Operation(summary = "LinkedHashSet 保序", description = "去重同时保持插入顺序")
    @PostMapping("/linkedhashset-order")
    public ApiResponse<ScenarioResult> linkedhashsetOrder() {
        return ApiResponse.success(setScenarioService.linkedhashsetOrder());
    }

    @Operation(summary = "TreeSet 排序", description = "自然排序与自定义 Comparator，不可比对象会抛异常")
    @PostMapping("/treeset-sort")
    public ApiResponse<ScenarioResult> treesetSort() {
        return ApiResponse.success(setScenarioService.treesetSort());
    }

    @Operation(summary = "equals/hashCode 契约", description = "违反契约导致的诡异去重与 hash 表定位失败")
    @PostMapping("/equals-hashcode-contract")
    public ApiResponse<ScenarioResult> equalsHashcodeContract() {
        return ApiResponse.success(setScenarioService.equalsHashcodeContract());
    }
}

package com.example.cg.list;

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
 * List 场景 REST 接口。
 *
 * 覆盖 ArrayList / LinkedList、subList 视图、ListIterator、fail-fast 迭代器。
 */
@Tag(name = "List 场景", description = "ArrayList / LinkedList / subList / ListIterator / fail-fast")
@RestController
@RequestMapping("/api/list")
@RequiredArgsConstructor
public class ListController {

    private final ListScenarioService listScenarioService;

    @Operation(summary = "ArrayList vs LinkedList", description = "对比随机访问、尾部追加、中间插入/删除的耗时差异")
    @PostMapping("/arraylist-vs-linkedlist")
    public ApiResponse<ScenarioResult> arraylistVsLinkedlist(
            @Parameter(description = "数据规模", example = "100000") @RequestParam(defaultValue = "100000") int size) {
        if (size < 1000 || size > 1000000) {
            return ApiResponse.error(400, "size 必须在 1000~1000000 之间");
        }
        return ApiResponse.success(listScenarioService.arraylistVsLinkedlist(size));
    }

    @Operation(summary = "subList 陷阱", description = "subList 是原 List 的视图，修改会相互影响")
    @PostMapping("/sublist-trap")
    public ApiResponse<ScenarioResult> sublistTrap() {
        return ApiResponse.success(listScenarioService.sublistTrap());
    }

    @Operation(summary = "ListIterator 双向迭代", description = "从两端遍历并在中间修改元素")
    @PostMapping("/listiterator")
    public ApiResponse<ScenarioResult> listIterator() {
        return ApiResponse.success(listScenarioService.listIterator());
    }

    @Operation(summary = "fail-fast 迭代器", description = "迭代过程中结构性修改触发 ConcurrentModificationException")
    @PostMapping("/iterator-failfast")
    public ApiResponse<ScenarioResult> iteratorFailfast() {
        return ApiResponse.success(listScenarioService.iteratorFailfast());
    }
}

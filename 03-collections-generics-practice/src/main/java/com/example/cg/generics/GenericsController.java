package com.example.cg.generics;

import com.example.cg.common.ApiResponse;
import com.example.cg.common.ScenarioResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 泛型场景 REST 接口。
 *
 * 覆盖 PECS、类型擦除、泛型方法、泛型 DAO、通配符限制。
 */
@Tag(name = "泛型场景", description = "PECS / 类型擦除 / 泛型方法 / 泛型 DAO")
@RestController
@RequestMapping("/api/generics")
@RequiredArgsConstructor
public class GenericsController {

    private final GenericsScenarioService genericsScenarioService;

    @Operation(summary = "PECS 原则", description = "<? extends T> 只读，<? super T> 只写")
    @PostMapping("/pecs")
    public ApiResponse<ScenarioResult> pecs() {
        return ApiResponse.success(genericsScenarioService.pecs());
    }

    @Operation(summary = "类型擦除与 raw type", description = "泛型在运行时擦除，raw type 破坏类型安全")
    @PostMapping("/type-erasure")
    public ApiResponse<ScenarioResult> typeErasure() {
        return ApiResponse.success(genericsScenarioService.typeErasure());
    }

    @Operation(summary = "泛型方法 + 类型推断", description = "定义 <T extends Comparable<T>> T findMax")
    @PostMapping("/generic-method")
    public ApiResponse<ScenarioResult> genericMethod() {
        return ApiResponse.success(genericsScenarioService.genericMethod());
    }

    @Operation(summary = "泛型 DAO / Service", description = "BaseRepository<T, ID> 与分层泛型约束")
    @PostMapping("/generic-dao")
    public ApiResponse<ScenarioResult> genericDao() {
        return ApiResponse.success(genericsScenarioService.genericDao());
    }
}

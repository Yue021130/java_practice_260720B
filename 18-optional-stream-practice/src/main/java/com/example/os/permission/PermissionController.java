package com.example.os.permission;

import com.example.os.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 03 菜单权限树接口。
 */
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/tree")
    @Operation(summary = "菜单权限树", description = "根据角色编码构建菜单树，演示 Optional.flatMap + Stream 递归。")
    public ApiResponse<Map<String, Object>> tree(
            @Parameter(description = "角色编码：admin / user / guest，为空按 guest 处理", example = "admin")
            @RequestParam(required = false) String roleCode) {
        return ApiResponse.ok(permissionService.tree(roleCode));
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记", description = "返回本场景的核心考点与常见陷阱。")
    public ApiResponse<Map<String, Object>> explain() {
        return ApiResponse.ok(permissionService.explain());
    }
}

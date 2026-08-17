package com.example.os.permission;

import com.example.os.domain.Menu;
import com.example.os.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 03 菜单权限树：演示 Optional.flatMap 解嵌套 + Stream 递归构建树形结构。
 *
 * <p>真实场景：RBAC 权限系统里，根据角色编码拿到权限菜单列表，再组装成前端需要的树形 JSON。
 * 这里用 Optional 处理“角色可能不存在”的情况，用 Stream 的 groupingBy + 递归把平铺菜单转成树。</p>
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final MockDataRepository repository;

    /**
     * 根据角色编码构建菜单树。
     *
     * @param roleCode 角色编码，可能为空
     * @return 树形菜单与构建过程说明
     */
    public Map<String, Object> tree(String roleCode) {
        // 1. Optional 处理可空角色：为空或空字符串时统一按 GUEST 兜底。
        String normalizedRole = Optional.ofNullable(roleCode)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse("guest");

        // 2. 模拟“根据角色查权限菜单”：真实业务会查 role_menu 关联表。
        List<Long> allowedMenuIds = getAllowedMenuIds(normalizedRole);

        // 3. flatMap 解嵌套：把 Optional<Menu> 转成 Stream<Menu>，再过滤出角色允许的菜单。
        List<Menu> allowedMenus = repository.getMenus().stream()
                .map(m -> Optional.ofNullable(m)
                        .filter(menu -> allowedMenuIds.contains(menu.getId())))
                // Java 8 中没有 Optional.stream()，用 flatMap(Optional::isPresent ? Stream.of : Stream.empty) 等价实现。
                .flatMap(opt -> opt.isPresent() ? java.util.stream.Stream.of(opt.get()) : java.util.stream.Stream.empty())
                .sorted(Comparator.comparing(Menu::getOrderNum))
                .collect(Collectors.toList());

        // 4. Stream 分组：按 parentId 分组，方便递归查找子节点。
        Map<Long, List<Menu>> menuMap = allowedMenus.stream()
                .collect(Collectors.groupingBy(
                        m -> Optional.ofNullable(m.getParentId()).orElse(0L),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 5. 递归构建树：从根节点（parentId=0）开始。
        List<Map<String, Object>> tree = buildTree(menuMap, 0L);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roleCode", normalizedRole);
        result.put("menuCount", allowedMenus.size());
        result.put("tree", tree);
        result.put("interviewNote", "Optional.flatMap 能把嵌套的可空对象展平；Stream.groupingBy 把平铺数据转成可按 parentId 递归的树。");
        return result;
    }

    /**
     * 模拟角色权限：不同角色能看到不同菜单。
     */
    private List<Long> getAllowedMenuIds(String roleCode) {
        switch (roleCode.toLowerCase()) {
            case "admin":
                // 管理员：所有菜单
                return repository.getMenus().stream()
                        .map(Menu::getId)
                        .collect(Collectors.toList());
            case "user":
                // 普通用户：只能看订单相关
                return java.util.Arrays.asList(2L, 21L);
            case "guest":
            default:
                // 游客：只能看用户列表
                return Collections.singletonList(111L);
        }
    }

    /**
     * 递归构建菜单树。
     */
    private List<Map<String, Object>> buildTree(Map<Long, List<Menu>> menuMap, Long parentId) {
        return Optional.ofNullable(menuMap.get(parentId))
                .orElse(Collections.emptyList())
                .stream()
                .map(menu -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", menu.getId());
                    node.put("name", menu.getName());
                    node.put("code", menu.getCode());
                    node.put("orderNum", menu.getOrderNum());
                    // 递归挂子节点：如果 menuMap 里没有该节点的子节点，返回空列表。
                    node.put("children", buildTree(menuMap, menu.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "菜单权限树");
        result.put("pattern", "Optional 规范化可空角色 → Stream 过滤 + groupingBy + 递归构建树");
        result.put("keyPoints", new String[]{
                "Optional.ofNullable(roleCode).map(trim).filter(非空).orElse(默认值) 统一处理空参",
                "Java 8 没有 Optional.stream()，可用 flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty()) 替代",
                "groupingBy 按 parentId 分组是构建树最常用的前置步骤",
                "递归方法用 Optional.ofNullable(map.get(key)).orElse(emptyList()) 防御空分支"
        });
        result.put("trap", "Tree 节点里的 children 如果没有子节点，建议返回空列表而不是 null，避免前端出现 undefined 分支。");
        return result;
    }
}

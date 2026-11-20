package com.example.epc.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.epc.entity.Dept;
import com.example.epc.entity.Role;
import com.example.epc.entity.RoleDataScope;
import com.example.epc.entity.User;
import com.example.epc.entity.UserRole;
import com.example.epc.mapper.DeptMapper;
import com.example.epc.mapper.RoleDataScopeMapper;
import com.example.epc.mapper.RoleMapper;
import com.example.epc.mapper.UserMapper;
import com.example.epc.mapper.UserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据范围解析器：userId → {@link DataScopeContext}。
 *
 * <p>解析规则（对应参考文档 2 的「生产级落地」）：</p>
 * <ol>
 *   <li>查用户所有角色各自的数据范围规则；</li>
 *   <li>多角色取「最宽」一条（{@link DataScopeType#isWiderThan}），
 *   不取交集——交集会让权限越配越小，排查成本高；</li>
 *   <li>DEPT_AND_CHILD 递归部门树收集所有子孙部门 id；</li>
 *   <li>SELF 不解析部门，上下文携带 userId 供拦截器拼 user_id 条件。</li>
 * </ol>
 *
 * <p>被 {@link com.example.epc.permission.PermissionCache} 缓存，
 * 本身不做缓存——解析逻辑保持无状态，缓存策略由外层统一管理。</p>
 */
@Component
public class DataScopeResolver {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleDataScopeMapper roleDataScopeMapper;
    private final DeptMapper deptMapper;

    public DataScopeResolver(UserMapper userMapper,
                             UserRoleMapper userRoleMapper,
                             RoleMapper roleMapper,
                             RoleDataScopeMapper roleDataScopeMapper,
                             DeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleDataScopeMapper = roleDataScopeMapper;
        this.deptMapper = deptMapper;
    }

    public DataScopeContext resolve(Long userId) {
        User user = userMapper.selectById(userId);

        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());

        // 多角色取最宽的数据范围
        DataScopeType widest = null;
        RoleDataScope chosen = null;
        if (!roleIds.isEmpty()) {
            for (RoleDataScope rule : roleDataScopeMapper.selectList(
                    new LambdaQueryWrapper<RoleDataScope>().in(RoleDataScope::getRoleId, roleIds))) {
                DataScopeType type = DataScopeType.valueOf(rule.getScopeType());
                if (type.isWiderThan(widest)) {
                    widest = type;
                    chosen = rule;
                }
            }
        }
        if (widest == null) {
            // 没配数据范围的角色默认仅本人——默认拒绝原则在数据层的体现
            widest = DataScopeType.SELF;
        }

        Set<Long> deptIds = null;
        switch (widest) {
            case ALL:
                deptIds = null;
                break;
            case CUSTOM:
                deptIds = parseCsv(chosen.getCustomDeptIds());
                break;
            case DEPT:
                deptIds = new HashSet<>();
                if (user != null && user.getDeptId() != null) {
                    deptIds.add(user.getDeptId());
                }
                break;
            case DEPT_AND_CHILD:
                deptIds = user == null ? new HashSet<>()
                        : collectSelfAndChildren(user.getDeptId());
                break;
            case SELF:
            default:
                deptIds = new HashSet<>();
                break;
        }
        return new DataScopeContext(widest, deptIds, userId);
    }

    /**
     * 收集部门及其全部子孙部门 id。
     * 部门表通常很小（几百行以内），这里一次性全量加载在内存里做 BFS，
     * 比「每查一层发一次 SQL」的递归查询更省往返，也更容易测试。
     */
    private Set<Long> collectSelfAndChildren(Long rootDeptId) {
        Set<Long> result = new HashSet<>();
        if (rootDeptId == null) {
            return result;
        }
        List<Dept> all = deptMapper.selectList(null);
        java.util.Map<Long, List<Long>> childrenMap = new java.util.HashMap<>();
        for (Dept d : all) {
            childrenMap.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(d.getId());
        }
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootDeptId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !result.add(current)) {
                continue;
            }
            queue.addAll(childrenMap.getOrDefault(current, new ArrayList<>()));
        }
        return result;
    }

    private Set<Long> parseCsv(String csv) {
        Set<Long> ids = new HashSet<>();
        if (csv == null || csv.isEmpty()) {
            return ids;
        }
        for (String s : csv.split(",")) {
            ids.add(Long.valueOf(s.trim()));
        }
        return ids;
    }
}

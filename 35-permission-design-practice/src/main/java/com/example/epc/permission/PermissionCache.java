package com.example.epc.permission;

import com.example.epc.datascope.DataScopeContext;
import com.example.epc.datascope.DataScopeResolver;
import com.example.epc.entity.Permission;
import com.example.epc.entity.Role;
import com.example.epc.entity.UserRole;
import com.example.epc.mapper.PermissionMapper;
import com.example.epc.mapper.RoleMapper;
import com.example.epc.mapper.RolePermissionMapper;
import com.example.epc.mapper.UserRoleMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限缓存：userId → 权限点集合 / 角色编码 / 数据范围上下文。
 *
 * <p>为什么缓存：Sa-Token 每次 @SaCheckPermission 校验、每次数据权限
 * SQL 注入都会查「用户有哪些权限」，每次都联表查库会把权限表打成热点。
 * 生产标准做法是缓存 + 变更失效（而非 TTL 兜底），这里两者都用：
 * Caffeine 10 分钟 TTL 兜底 + 用户/角色/权限/数据范围变更时主动 evict。</p>
 *
 * <p>一致性问题（八股）：改权限后缓存里的旧权限还生效怎么办？
 * 答案就是 evict——所有写入口（UserService/RoleService/PermissionService）
 * 在变更后调用 evict(userId) 或 evictAll()。admin 场景可以粗暴
 * evictAll；精细化做法是按角色反查在线用户逐个失效。</p>
 */
@Component
public class PermissionCache {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final DataScopeResolver dataScopeResolver;

    /** 单机进程内缓存；集群环境应换 Redis 并在 evict 时广播 */
    private final Cache<Long, CachedPermission> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public PermissionCache(UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper,
                           RolePermissionMapper rolePermissionMapper,
                           PermissionMapper permissionMapper,
                           DataScopeResolver dataScopeResolver) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
        this.dataScopeResolver = dataScopeResolver;
    }

    public Set<String> getPermissionCodes(Long userId) {
        return load(userId).perms;
    }

    public Set<String> getRoleCodes(Long userId) {
        return load(userId).roles;
    }

    public DataScopeContext getDataScope(Long userId) {
        return load(userId).dataScope;
    }

    private CachedPermission load(Long userId) {
        return cache.get(userId, this::doLoad);
    }

    private CachedPermission doLoad(Long userId) {
        // ① 用户的角色
        List<Long> roleIds = userRoleMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                                .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return new CachedPermission(Collections.emptySet(), Collections.emptySet(),
                    dataScopeResolver.resolve(userId));
        }
        Set<String> roles = roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getRoleCode).collect(Collectors.toSet());
        // ② 角色 → 权限点
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.epc.entity.RolePermission>()
                                .in(com.example.epc.entity.RolePermission::getRoleId, roleIds))
                .stream().map(com.example.epc.entity.RolePermission::getPermissionId)
                .distinct().collect(Collectors.toList());
        Set<String> perms = permissionIds.isEmpty() ? new HashSet<>()
                : permissionMapper.selectBatchIds(permissionIds).stream()
                .map(Permission::getPerm).collect(Collectors.toSet());
        return new CachedPermission(perms, roles, dataScopeResolver.resolve(userId));
    }

    /** 变更主动失效：按用户 */
    public void evict(Long userId) {
        cache.invalidate(userId);
    }

    /** 变更主动失效：全量（角色/权限变更影响面大时用） */
    public void evictAll() {
        cache.invalidateAll();
    }

    /** 缓存的内部快照 */
    static class CachedPermission {
        final Set<String> perms;
        final Set<String> roles;
        final DataScopeContext dataScope;

        CachedPermission(Set<String> perms, Set<String> roles, DataScopeContext dataScope) {
            this.perms = perms;
            this.roles = roles;
            this.dataScope = dataScope;
        }
    }
}

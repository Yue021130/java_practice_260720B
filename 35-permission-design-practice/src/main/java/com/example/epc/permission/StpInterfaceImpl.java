package com.example.epc.permission;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限供给接口：把「用户有哪些权限/角色」对接到 Sa-Token。
 *
 * <p>Sa-Token 每次执行 @SaCheckPermission("order:list") 时都会回调
 * {@link #getPermissionList}——这是权限体系里最热的读路径，
 * 所以这里走 {@link PermissionCache} 而不是直接查库。</p>
 *
 * <p>这样设计的收益：权限存储结构（五表怎么关联）只对
 * PermissionCache 可见，鉴权注解层面完全不感知表结构——
 * 以后把 RBAC 换成 ABAC，注解和前端都不用动。</p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final PermissionCache permissionCache;

    public StpInterfaceImpl(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>(permissionCache.getPermissionCodes(Long.valueOf(loginId.toString())));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>(permissionCache.getRoleCodes(Long.valueOf(loginId.toString())));
    }
}

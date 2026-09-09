package com.example.rbac.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.example.rbac.service.PermissionService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据源：
 * StpUtil.checkRole / checkPermission 时回调这里，从数据库加载当前用户的角色与权限点。
 *
 * <p>这保证了 @SaCheckPermission("order:delete") 这类注解校验的始终是
 * 数据库里的最新权限配置（权限可配置化），改库即生效，无需改代码。</p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final PermissionService permissionService;

    public StpInterfaceImpl(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.listPermissionCodesByUserId(Long.valueOf(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionService.listRoleCodesByUserId(Long.valueOf(loginId.toString()));
    }
}

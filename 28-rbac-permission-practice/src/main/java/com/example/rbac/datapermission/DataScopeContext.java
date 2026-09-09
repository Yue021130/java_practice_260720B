package com.example.rbac.datapermission;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rbac.entity.SysRole;
import com.example.rbac.entity.SysUser;
import com.example.rbac.entity.SysUserRole;
import com.example.rbac.mapper.SysRoleMapper;
import com.example.rbac.mapper.SysUserMapper;
import com.example.rbac.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限上下文加载器：
 * 每次查询时根据当前登录用户，计算出其数据权限范围。
 *
 * <p>取用户所有角色中最大的 data_scope（数值越大可见范围越大），
 * 未登录或查询失败时放行（GLOBAL），避免把系统自身查询堵死。</p>
 */
@Component
public class DataScopeContext {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public DataScopeContext(SysUserMapper userMapper, SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 获取当前登录用户的数据权限范围
     */
    public DataScopeInfo current() {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                return DataScopeInfo.global();
            }
            List<Long> roleIds = userRoleMapper.selectList(
                            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                    .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            if (roleIds.isEmpty()) {
                return new DataScopeInfo(1, userId, user.getDeptId());
            }
            Integer maxScope = roleMapper.selectBatchIds(roleIds).stream()
                    .mapToInt(SysRole::getDataScope)
                    .max().orElse(1);
            return new DataScopeInfo(maxScope, userId, user.getDeptId());
        } catch (Exception e) {
            // 未登录 / 系统内部调用：不附加数据权限条件
            return DataScopeInfo.global();
        }
    }
}

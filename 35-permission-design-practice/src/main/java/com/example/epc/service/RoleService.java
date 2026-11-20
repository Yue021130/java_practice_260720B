package com.example.epc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.epc.common.BusinessException;
import com.example.epc.dto.RoleSaveDTO;
import com.example.epc.dto.RoleVO;
import com.example.epc.entity.Role;
import com.example.epc.entity.RoleDataScope;
import com.example.epc.entity.RolePermission;
import com.example.epc.mapper.RoleDataScopeMapper;
import com.example.epc.mapper.RoleMapper;
import com.example.epc.mapper.RolePermissionMapper;
import com.example.epc.permission.PermissionCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色 Service：角色 CRUD + 权限点全量维护 + 数据范围规则维护。
 *
 * <p>角色是权限的「放大器」：一个角色变更，挂这个角色的一批用户的
 * 权限集合全部失真。所以这里不做精细化失效，直接
 * {@link PermissionCache#evictAll()} 粗暴全失效——角色管理是低频
 * 操作，全量清一次的代价（下一个请求回源）完全可以接受。</p>
 */
@Slf4j
@Service
public class RoleService {

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RolePermissionMapper rolePermissionMapper;
    @Resource
    private RoleDataScopeMapper roleDataScopeMapper;
    @Resource
    private PermissionCache permissionCache;

    public Page<RoleVO> page(long current, long size, String roleName) {
        Page<Role> page = roleMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Role>()
                        .like(StringUtils.hasText(roleName), Role::getRoleName, roleName)
                        .orderByDesc(Role::getCreateTime));
        Page<RoleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    public RoleVO detail(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return toVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(RoleSaveDTO dto) {
        Long exists = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, dto.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException("角色编码已存在");
        }
        Role role = new Role();
        BeanUtils.copyProperties(dto, role);
        roleMapper.insert(role);

        savePermissions(role.getId(), dto.getPermissionIds());
        upsertDataScope(role.getId(), dto.getScopeType(), dto.getCustomDeptIds());
        // 角色影响面大，粗暴全失效（见类注释）
        permissionCache.evictAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(RoleSaveDTO dto) {
        Role role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setRoleName(dto.getRoleName());
        roleMapper.updateById(role);

        // 权限点全量覆盖：先删后插
        savePermissions(role.getId(), dto.getPermissionIds());
        upsertDataScope(role.getId(), dto.getScopeType(), dto.getCustomDeptIds());
        permissionCache.evictAll();
    }

    public void delete(Long id) {
        if (roleMapper.selectById(id) == null) {
            throw new BusinessException("角色不存在");
        }
        // @TableLogic 逻辑删除；关联的 role_permission / user_role 保留，
        // 重建同名角色时数据可溯源
        roleMapper.deleteById(id);
        permissionCache.evictAll();
    }

    /** 权限点全量覆盖：删除该角色全部旧关联，按传入集合重新插入 */
    private void savePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    /** 数据范围规则 upsert：一个角色一条，存在即更新，不存在则插入 */
    private void upsertDataScope(Long roleId, String scopeType, String customDeptIds) {
        RoleDataScope scope = roleDataScopeMapper.selectOne(
                new LambdaQueryWrapper<RoleDataScope>().eq(RoleDataScope::getRoleId, roleId));
        if (scope == null) {
            scope = new RoleDataScope();
            scope.setRoleId(roleId);
            scope.setScopeType(scopeType);
            scope.setCustomDeptIds(customDeptIds);
            roleDataScopeMapper.insert(scope);
        } else {
            scope.setScopeType(scopeType);
            scope.setCustomDeptIds(customDeptIds);
            roleDataScopeMapper.updateById(scope);
        }
    }

    private RoleVO toVO(Role role) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()))
                .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        vo.setPermissionIds(permissionIds);
        RoleDataScope scope = roleDataScopeMapper.selectOne(
                new LambdaQueryWrapper<RoleDataScope>().eq(RoleDataScope::getRoleId, role.getId()));
        if (scope != null) {
            vo.setScopeType(scope.getScopeType());
            vo.setCustomDeptIds(scope.getCustomDeptIds());
        }
        return vo;
    }
}

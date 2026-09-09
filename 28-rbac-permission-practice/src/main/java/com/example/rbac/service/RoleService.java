package com.example.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rbac.common.BusinessException;
import com.example.rbac.dto.RoleSaveDTO;
import com.example.rbac.entity.SysRole;
import com.example.rbac.entity.SysRolePermission;
import com.example.rbac.mapper.SysRoleMapper;
import com.example.rbac.mapper.SysRolePermissionMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务：角色 CRUD + 权限点分配（权限可配置化的落点）
 */
@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public RoleService(SysRoleMapper roleMapper, SysRolePermissionMapper rolePermissionMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    /**
     * 查询角色已拥有的权限点 ID
     */
    public List<Long> listPermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(RoleSaveDTO dto) {
        SysRole role;
        if (dto.getId() == null) {
            Long count = roleMapper.selectCount(
                    new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, dto.getCode()));
            if (count > 0) {
                throw new BusinessException(400, "角色编码已存在");
            }
            role = new SysRole();
            BeanUtils.copyProperties(dto, role);
            role.setId(null);
            roleMapper.insert(role);
        } else {
            role = roleMapper.selectById(dto.getId());
            if (role == null) {
                throw new BusinessException(400, "角色不存在");
            }
            role.setCode(dto.getCode());
            role.setName(dto.getName());
            role.setDataScope(dto.getDataScope());
            roleMapper.updateById(role);
        }
        if (dto.getPermissionIds() != null) {
            assignPermissions(role.getId(), dto.getPermissionIds());
        }
    }

    /**
     * 为角色重新分配权限点：删除旧关联、写入新关联。
     * 由于 StpInterfaceImpl 每次校验都实时查库，分配后下一次请求立即生效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(400, "角色不存在");
        }
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        permissionIds.stream().distinct().forEach(permissionId -> {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionMapper.insert(rp);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id == 1L) {
            throw new BusinessException(400, "内置超级管理员角色不可删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
    }
}

package com.example.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rbac.dto.PermissionVO;
import com.example.rbac.entity.SysPermission;
import com.example.rbac.entity.SysRole;
import com.example.rbac.entity.SysRolePermission;
import com.example.rbac.entity.SysUserRole;
import com.example.rbac.mapper.SysPermissionMapper;
import com.example.rbac.mapper.SysRoleMapper;
import com.example.rbac.mapper.SysRolePermissionMapper;
import com.example.rbac.mapper.SysUserRoleMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限查询服务：为 Sa-Token 鉴权与菜单树提供数据源
 */
@Service
public class PermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public PermissionService(SysPermissionMapper permissionMapper,
                             SysRoleMapper roleMapper,
                             SysUserRoleMapper userRoleMapper,
                             SysRolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * 查询用户的角色编码集合，如 [ADMIN]
     */
    public List<String> listRoleCodesByUserId(Long userId) {
        List<Long> roleIds = listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的权限点编码集合，如 [order:list, order:delete]
     */
    public List<String> listPermissionCodesByUserId(Long userId) {
        List<Long> roleIds = listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的角色 ID 集合
     */
    public List<Long> listRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    /**
     * 全量权限树（角色管理页用于勾选分配）
     */
    public List<PermissionVO> permissionTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort));
        return buildTree(all, 0L);
    }

    /**
     * 当前登录用户可见的菜单树（动态路由数据源）
     */
    public List<PermissionVO> menuTreeByUserId(Long userId) {
        List<Long> roleIds = listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysPermission> menus = permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(p -> "menu".equals(p.getType()))
                .sorted((a, b) -> a.getSort() - b.getSort())
                .collect(Collectors.toList());
        return buildTree(menus, 0L);
    }

    private List<PermissionVO> buildTree(List<SysPermission> all, Long parentId) {
        Map<Long, List<SysPermission>> groupByParent = all.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));
        return buildChildren(groupByParent, parentId);
    }

    private List<PermissionVO> buildChildren(Map<Long, List<SysPermission>> groupByParent, Long parentId) {
        return groupByParent.getOrDefault(parentId, new ArrayList<>()).stream().map(p -> {
            PermissionVO vo = new PermissionVO();
            BeanUtils.copyProperties(p, vo);
            vo.setChildren(buildChildren(groupByParent, p.getId()));
            return vo;
        }).collect(Collectors.toList());
    }
}

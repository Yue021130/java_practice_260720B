package com.example.epc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.epc.common.BusinessException;
import com.example.epc.dto.MenuVO;
import com.example.epc.dto.PermissionSaveDTO;
import com.example.epc.dto.PermissionVO;
import com.example.epc.entity.Permission;
import com.example.epc.entity.RolePermission;
import com.example.epc.entity.UserRole;
import com.example.epc.mapper.PermissionMapper;
import com.example.epc.mapper.RolePermissionMapper;
import com.example.epc.mapper.UserRoleMapper;
import com.example.epc.permission.PermissionCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限 Service：权限点（菜单/按钮）的 CRUD 与按用户查询。
 *
 * <p>任何权限结构变更后必须 {@link PermissionCache#evictAll()}——
 * 缓存里存的是 permission 编码集合，结构变了旧缓存就是脏数据。</p>
 */
@Slf4j
@Service
public class PermissionService {

    @Resource
    private PermissionMapper permissionMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RolePermissionMapper rolePermissionMapper;
    @Resource
    private PermissionCache permissionCache;

    /** 全部权限点（平铺，前端建树或下拉选择用） */
    public List<Permission> list() {
        return permissionMapper.selectList(null);
    }

    /** 全部权限点转树（权限管理页渲染用） */
    public List<PermissionVO> tree() {
        List<Permission> all = list();
        List<PermissionVO> vos = all.stream().map(this::toVO).collect(Collectors.toList());
        return buildTree(vos);
    }

    /**
     * 当前用户可见的菜单树：sys_user_role → sys_role_permission → sys_permission，
     * 过滤 type = MENU 后转 {@link MenuVO} 树，登录接口下发给前端。
     */
    public List<MenuVO> listMenuTreeByUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId)
                .distinct().collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<MenuVO> menus = permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(p -> "MENU".equals(p.getType()))
                .map(this::toMenuVO)
                .collect(Collectors.toList());
        return buildMenuTree(menus);
    }

    /** 当前用户拥有的 BUTTON 类型权限点标识集合（登录下发 + 前端按钮显隐） */
    public List<String> permsByUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId)
                .distinct().collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(p -> "BUTTON".equals(p.getType()) && p.getPerm() != null)
                .map(Permission::getPerm)
                .sorted()
                .collect(Collectors.toList());
    }

    public void save(PermissionSaveDTO dto) {
        Permission entity = new Permission();
        BeanUtils.copyProperties(dto, entity);
        permissionMapper.insert(entity);
        // 权限结构变更 → 全量失效缓存（所有用户的权限集合都可能受影响）
        permissionCache.evictAll();
    }

    public void update(PermissionSaveDTO dto) {
        Permission exist = permissionMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException("权限不存在");
        }
        Permission entity = new Permission();
        BeanUtils.copyProperties(dto, entity);
        permissionMapper.updateById(entity);
        permissionCache.evictAll();
    }

    public void delete(Long id) {
        // 有子节点不允许删，防止留孤儿数据
        Long children = permissionMapper.selectCount(
                new LambdaQueryWrapper<Permission>().eq(Permission::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("存在子权限，不能删除");
        }
        permissionMapper.deleteById(id);
        permissionCache.evictAll();
    }

    private PermissionVO toVO(Permission p) {
        PermissionVO vo = new PermissionVO();
        BeanUtils.copyProperties(p, vo);
        return vo;
    }

    private MenuVO toMenuVO(Permission p) {
        MenuVO vo = new MenuVO();
        BeanUtils.copyProperties(p, vo);
        return vo;
    }

    /** 平铺 VO → 树：按 parentId 分组挂到父节点下，顶级为 parentId = 0 */
    private List<PermissionVO> buildTree(List<PermissionVO> all) {
        Map<Long, List<PermissionVO>> byParent = all.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));
        return attachPermissionChildren(byParent, 0L);
    }

    private List<PermissionVO> attachPermissionChildren(Map<Long, List<PermissionVO>> byParent, Long parentId) {
        List<PermissionVO> children = byParent.getOrDefault(parentId, new ArrayList<>()).stream()
                .sorted(Comparator.comparing(p -> p.getSortOrder() == null ? 0 : p.getSortOrder()))
                .collect(Collectors.toList());
        for (PermissionVO child : children) {
            child.setChildren(attachPermissionChildren(byParent, child.getId()));
        }
        return children;
    }

    private List<MenuVO> buildMenuTree(List<MenuVO> all) {
        Map<Long, List<MenuVO>> byParent = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return attachMenuChildren(byParent, 0L);
    }

    private List<MenuVO> attachMenuChildren(Map<Long, List<MenuVO>> byParent, Long parentId) {
        List<MenuVO> children = byParent.getOrDefault(parentId, new ArrayList<>()).stream()
                .sorted(Comparator.comparing(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .collect(Collectors.toList());
        for (MenuVO child : children) {
            child.setChildren(attachMenuChildren(byParent, child.getId()));
        }
        return children;
    }
}

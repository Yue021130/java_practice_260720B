package com.example.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rbac.common.BusinessException;
import com.example.rbac.dto.UserPageVO;
import com.example.rbac.dto.UserSaveDTO;
import com.example.rbac.entity.SysRole;
import com.example.rbac.entity.SysUser;
import com.example.rbac.entity.SysUserRole;
import com.example.rbac.mapper.SysRoleMapper;
import com.example.rbac.mapper.SysUserMapper;
import com.example.rbac.mapper.SysUserRoleMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public UserService(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper, SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    public Page<UserPageVO> page(long current, long size, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        List<Long> userIds = page.getRecords().stream().map(SysUser::getId).collect(Collectors.toList());
        Map<Long, List<Long>> roleIdMap = userIds.isEmpty() ? Collections.emptyMap()
                : userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
        Map<Long, String> roleNameMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getName));

        Page<UserPageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(user -> {
            UserPageVO vo = new UserPageVO();
            BeanUtils.copyProperties(user, vo);
            List<Long> roleIds = roleIdMap.getOrDefault(user.getId(), Collections.emptyList());
            vo.setRoleIds(roleIds);
            vo.setRoleNames(roleIds.stream().map(roleNameMap::get).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(UserSaveDTO dto) {
        SysUser user;
        if (dto.getId() == null) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
            if (count > 0) {
                throw new BusinessException(400, "用户名已存在");
            }
            if (!StringUtils.hasText(dto.getPassword())) {
                throw new BusinessException(400, "新增用户必须设置密码");
            }
            user = new SysUser();
            BeanUtils.copyProperties(dto, user);
            user.setId(null);
            user.setStatus(1);
            user.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8)));
            userMapper.insert(user);
        } else {
            user = userMapper.selectById(dto.getId());
            if (user == null) {
                throw new BusinessException(400, "用户不存在");
            }
            user.setNickname(dto.getNickname());
            user.setDeptId(dto.getDeptId());
            if (StringUtils.hasText(dto.getPassword())) {
                user.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8)));
            }
            userMapper.updateById(user);
        }
        // 重建用户-角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        if (dto.getRoleIds() != null) {
            dto.getRoleIds().forEach(roleId -> {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            });
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id == 1L) {
            throw new BusinessException(400, "内置管理员不可删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }
}

package com.example.epc.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.epc.common.BusinessException;
import com.example.epc.dto.UserSaveDTO;
import com.example.epc.dto.UserVO;
import com.example.epc.entity.Dept;
import com.example.epc.entity.Role;
import com.example.epc.entity.User;
import com.example.epc.entity.UserRole;
import com.example.epc.mapper.DeptMapper;
import com.example.epc.mapper.RoleMapper;
import com.example.epc.mapper.UserMapper;
import com.example.epc.mapper.UserRoleMapper;
import com.example.epc.permission.PermissionCache;
import com.example.epc.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户 Service：用户 CRUD + 角色分配。
 *
 * <p>缓存一致性：用户的角色/权限来自 sys_user_role 关联，任何
 * save/update/delete/resetPassword 之后都要让该用户的
 * {@link PermissionCache} 条目失效（密码变了 token 里的信息虽不变，
 * 但角色变了权限立刻要生效）。</p>
 */
@Slf4j
@Service
public class UserService {

    /** 重置密码时的统一初始密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private PasswordUtil passwordUtil;
    @Resource
    private PermissionCache permissionCache;

    public Page<UserVO> page(long current, long size, String username) {
        Page<User> page = userMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<User>()
                        .like(StringUtils.hasText(username), User::getUsername, username)
                        .orderByDesc(User::getCreateTime));
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    public UserVO detail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(UserSaveDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setDeptId(dto.getDeptId());
        // 密码可空：新增不给默认 123456（演示环境约定，生产应强制改密）
        user.setPassword(passwordUtil.encode(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD));
        user.setStatus(1);
        userMapper.insert(user);

        assignRoles(user.getId(), dto.getRoleIds());
        // 该用户缓存从未建立过，evict 是防御性写法（幂等，无副作用）
        permissionCache.evict(user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(UserSaveDTO dto) {
        User user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 先失效缓存再改库：并发下宁可多一次回源，也不让旧权限多生效一秒
        permissionCache.evict(user.getId());

        user.setNickname(dto.getNickname());
        user.setDeptId(dto.getDeptId());
        // 密码为空表示不修改；用户名创建后不允许变更（登录标识稳定性）
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordUtil.encode(dto.getPassword()));
        }
        userMapper.updateById(user);

        assignRoles(user.getId(), dto.getRoleIds());
    }

    public void delete(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("用户不存在");
        }
        if (StpUtil.getLoginIdAsLong() == id) {
            throw new BusinessException("不能删除当前登录用户");
        }
        // @TableLogic 逻辑删除：关联的 sys_user_role 保留，重启用时角色还在
        userMapper.deleteById(id);
        permissionCache.evict(id);
    }

    /** 重置密码为初始密码 123456 */
    public void resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordUtil.encode(DEFAULT_PASSWORD));
        userMapper.updateById(user);
        permissionCache.evict(id);
    }

    /** 分配角色：全量覆盖（先删旧关联再插新），必须随业务写事务一起提交 */
    private void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    /** 实体 → VO，顺带填充部门名与角色信息 */
    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        org.springframework.beans.BeanUtils.copyProperties(user, vo);
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectById(user.getDeptId());
            vo.setDeptName(dept == null ? null : dept.getName());
        }
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        vo.setRoleIds(roleIds);
        if (!roleIds.isEmpty()) {
            Map<Long, String> roleNames = roleMapper.selectBatchIds(roleIds).stream()
                    .collect(Collectors.toMap(Role::getId, Role::getRoleName, (a, b) -> a));
            vo.setRoleNames(roleIds.stream()
                    .map(rid -> roleNames.getOrDefault(rid, ""))
                    .collect(Collectors.toList()));
        } else {
            vo.setRoleNames(Collections.emptyList());
        }
        return vo;
    }
}

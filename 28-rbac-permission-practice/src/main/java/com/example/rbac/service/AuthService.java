package com.example.rbac.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rbac.common.BusinessException;
import com.example.rbac.dto.LoginDTO;
import com.example.rbac.dto.LoginVO;
import com.example.rbac.entity.SysUser;
import com.example.rbac.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/**
 * 认证服务：登录 / 登出 / 当前用户信息
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PermissionService permissionService;

    public AuthService(SysUserMapper userMapper, PermissionService permissionService) {
        this.userMapper = userMapper;
        this.permissionService = permissionService;
    }

    /**
     * 登录：校验用户名密码，签发 Sa-Token JWT（无状态）
     */
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        String md5 = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!md5.equalsIgnoreCase(user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        // Sa-Token 签发 JWT，loginId = userId
        StpUtil.login(user.getId());
        return buildLoginVO(user, StpUtil.getTokenValue());
    }

    /**
     * 当前登录用户信息
     */
    public LoginVO currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return buildLoginVO(user, null);
    }

    /**
     * 退出登录
     */
    public void logout() {
        StpUtil.logout();
    }

    private LoginVO buildLoginVO(SysUser user, String token) {
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(new HashSet<>(permissionService.listRoleCodesByUserId(user.getId())))
                .permissions(new HashSet<>(permissionService.listPermissionCodesByUserId(user.getId())))
                .build();
    }
}

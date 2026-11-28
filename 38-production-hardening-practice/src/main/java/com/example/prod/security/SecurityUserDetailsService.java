package com.example.prod.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.prod.entity.User;
import com.example.prod.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 从数据库加载用户与权限。
 *
 * <p>权限口径（本章为控制规模做的简化）：角色 → 权限点用常量映射。
 * ADMIN 拥有 order:read/order:write，USER 只有 order:read。
 * 生产应接 RBAC 五表（35 章已完整实现），这里注释标明边界。</p>
 */
@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public SecurityUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        // 简化口径：角色决定权限集合（生产接 RBAC 五表，见第35章）
        List<SimpleGrantedAuthority> authorities;
        if ("ADMIN".equals(user.getRole())) {
            authorities = Arrays.asList(
                    new SimpleGrantedAuthority("order:read"),
                    new SimpleGrantedAuthority("order:write"));
        } else {
            authorities = java.util.Collections.singletonList(
                    new SimpleGrantedAuthority("order:read"));
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), authorities);
    }
}

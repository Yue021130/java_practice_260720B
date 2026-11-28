package com.example.prod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.prod.dto.LoginDTO;
import com.example.prod.dto.LoginVO;
import com.example.prod.entity.User;
import com.example.prod.mapper.UserMapper;
import com.example.prod.security.JwtService;
import com.example.prod.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 认证服务：登录签发 JWT、登出拉黑 jti、查询当前用户
 *
 * <p>登录失败（BadCredentialsException）原样上抛，由 GlobalExceptionHandler 转 401；
 * 所有认证动作发审计事件（AuditEventRepository 由并行开发的 AuditConfig 提供）。</p>
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditEventRepository auditEventRepository;
    private final TokenBlacklist tokenBlacklist;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserMapper userMapper,
                       AuditEventRepository auditEventRepository,
                       TokenBlacklist tokenBlacklist) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.auditEventRepository = auditEventRepository;
        this.tokenBlacklist = tokenBlacklist;
    }

    /**
     * 登录：校验用户名密码（SecurityUserDetailsService 接管），成功后签发 JWT 并记审计
     *
     * @param ip 客户端 IP（X-Forwarded-For 优先），写入审计上下文
     */
    public LoginVO login(LoginDTO dto, String ip) {
        // 失败抛 BadCredentialsException，原样上抛，全局异常处理器兜底 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        String token = jwtService.generate(dto.getUsername());
        // Java 8 没有 Map.of，用 Collections.singletonMap；必须显式声明 <String, Object> 以匹配 AuditEvent 构造器
        auditEventRepository.add(new AuditEvent(dto.getUsername(), "LOGIN_SUCCESS",
                Collections.<String, Object>singletonMap("ip", ip)));

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        User user = findByUsername(dto.getUsername());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setRole(user.getRole());
        }
        // tokenType 字段默认即 "Bearer"
        return vo;
    }

    /**
     * 登出：解析并校验 token，把 jti 按剩余有效期拉黑（幂等，重复登出直接覆盖），记审计
     */
    public void logout(String authorizationHeader) {
        String token = jwtService.extractToken(authorizationHeader);
        Jws<Claims> jws = jwtService.parse(token); // 非法/过期 token 抛 JwtException，走全局异常处理
        Claims body = jws.getBody();
        String jti = body.getId();
        long remaining = body.getExpiration().getTime() - System.currentTimeMillis();
        // 已过期的 token 本就无法通过校验，无需拉黑；剩余时间才加入黑名单
        if (remaining > 0) {
            tokenBlacklist.add(jti, remaining);
        }
        auditEventRepository.add(new AuditEvent(body.getSubject(), "LOGOUT",
                Collections.<String, Object>singletonMap("jti", jti)));
    }

    /**
     * 当前用户信息（token 字段为 null，tokenType 保持默认 Bearer）
     */
    public LoginVO me(String username) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        LoginVO vo = new LoginVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}

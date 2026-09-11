package com.example.vmp.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vmp.common.BusinessException;
import com.example.vmp.dto.LoginDTO;
import com.example.vmp.dto.LoginVO;
import com.example.vmp.entity.User;
import com.example.vmp.mapper.UserMapper;
import com.example.vmp.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 认证 Service：登录、登出、当前用户信息。
 *
 * <p>Sa-Token JWT 无状态流程：</p>
 * <ol>
 *   <li>校验用户名密码（BCrypt）；</li>
 *   <li>StpUtil.login(userId) —— 由于已切换为 StpLogicJwtForStateless，
 *       这里直接生成 JWT 字符串，不落地任何存储；</li>
 *   <li>StpUtil.getTokenValue() 取出 JWT 返回给前端，前端后续请求放入 Authorization 头。</li>
 * </ol>
 */
@Slf4j
@Service
public class AuthService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private PasswordUtil passwordUtil;

    /**
     * 登录：校验通过后签发 JWT。
     */
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 无状态模式：只生成 JWT，不保存会话；再次登录不会被"顶号"
        StpUtil.login(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        log.info("用户 [{}] 登录成功", user.getUsername());
        return vo;
    }

    /**
     * 登出：无状态 JWT 服务端无会话可删，这里仅打印日志。
     * 生产实践是把 token 加入黑名单（Redis），本章保持简单。
     */
    public void logout() {
        if (StpUtil.isLogin()) {
            log.info("用户 [{}] 登出", StpUtil.getLoginId());
        }
    }

    /**
     * 获取当前登录用户信息（id 从 JWT 中解析，无需查库）。
     */
    public User currentUser() {
        Long loginId = Long.valueOf(StpUtil.getLoginId().toString());
        User user = userMapper.selectById(loginId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已删除");
        }
        return user;
    }
}

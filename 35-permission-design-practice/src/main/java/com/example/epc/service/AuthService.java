package com.example.epc.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.epc.common.BusinessException;
import com.example.epc.dto.LoginDTO;
import com.example.epc.dto.LoginVO;
import com.example.epc.entity.User;
import com.example.epc.mapper.UserMapper;
import com.example.epc.oplog.LoginLogService;
import com.example.epc.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private PermissionService permissionService;

    /**
     * 登录：校验通过后签发 JWT，并记录登录日志、下发菜单树与按钮权限点。
     *
     * <p>登录日志要求成败都记——失败也要留下痕迹（爆破排查靠的就是
     * 失败日志），所以校验异常 catch 住先落日志再原样抛出。</p>
     */
    public LoginVO login(LoginDTO dto) {
        String ip = currentIp();
        try {
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

            loginLogService.record(dto.getUsername(), ip, true, null);

            LoginVO vo = new LoginVO();
            vo.setToken(StpUtil.getTokenValue());
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            // 一次下发可见菜单树与按钮权限点，前端据此渲染菜单/控制按钮显隐
            vo.setMenus(permissionService.listMenuTreeByUser(user.getId()));
            vo.setPerms(permissionService.permsByUser(user.getId()));
            log.info("用户 [{}] 登录成功", user.getUsername());
            return vo;
        } catch (BusinessException e) {
            // 登录失败同样留痕：用户名、IP、失败原因，安全审计的基本盘
            loginLogService.record(dto.getUsername(), ip, false, e.getMessage());
            throw e;
        }
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

    /** 从当前请求上下文取客户端 IP：优先 X-Forwarded-For（反向代理场景），兜底 remoteAddr */
    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "-";
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return ip.split(",")[0].trim();
        } catch (Exception e) {
            return "-";
        }
    }
}

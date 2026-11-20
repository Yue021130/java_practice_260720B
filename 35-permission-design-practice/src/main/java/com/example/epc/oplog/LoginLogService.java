package com.example.epc.oplog;

import com.example.epc.entity.LoginLog;
import com.example.epc.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志：谁、什么时候、哪个 IP、成功与否。
 * 安全审计的基本盘——异常登录排查（爆破、异地）都靠它。
 */
@Service
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public LoginLogService(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    public void record(String username, String ip, boolean success, String message) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setIp(ip);
        log.setSuccess(success ? 1 : 0);
        log.setMessage(success ? null : message);
        log.setCreateTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}

package com.example.satoken.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限数据接口。
 *
 * Sa-Token 通过 StpInterface 从业务层获取账号的权限码与角色；
 * 本教学项目把权限/角色存在 Session 中，演示登录后写入权限的流程。
 */
@Component
public class CustomStpInterface implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) session.get("permList");
        return perms == null ? new ArrayList<>() : perms;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) session.get("roleList");
        return roles == null ? new ArrayList<>() : roles;
    }
}

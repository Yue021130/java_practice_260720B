package com.example.vmp.config;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 配置：切换为 JWT 无状态 StpLogic。
 *
 * <p>八股：JWT 三种模式 —— Simple（只是换了 Token 风格，仍校验持久存储）、
 * Mixin（混入 jwt 属性，仍需存储校验）、Stateless（完全无状态，不读存储，最纯粹的 JWT）。</p>
 *
 * <p>本章使用 StpLogicJwtForStateless：登录后签发 JWT 直接返回给前端，
 * 服务端不保存任何会话，天然支持水平扩展；代价是无法主动踢人下线（需要 Redis 存储方案）。</p>
 */
@Configuration
public class SaTokenConfig {

    @Autowired
    public void setStpLogic() {
        StpUtil.setStpLogic(new StpLogicJwtForStateless());
    }
}

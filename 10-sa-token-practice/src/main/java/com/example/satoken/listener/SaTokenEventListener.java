package com.example.satoken.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 全局 Sa-Token 事件监听器。
 *
 * 用于记录登录、注销、踢下线、封禁等关键事件，演示框架生命周期钩子。
 */
@Slf4j
@Component
public class SaTokenEventListener implements SaTokenListener {

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        log.info("[Sa-Token 事件] 登录：loginType={}, loginId={}, tokenValue={}", loginType, loginId, tokenValue);
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        log.info("[Sa-Token 事件] 注销：loginType={}, loginId={}, tokenValue={}", loginType, loginId, tokenValue);
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        log.info("[Sa-Token 事件] 踢下线：loginType={}, loginId={}, tokenValue={}", loginType, loginId, tokenValue);
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        log.info("[Sa-Token 事件] 被顶下线：loginType={}, loginId={}, tokenValue={}", loginType, loginId, tokenValue);
    }

    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        log.info("[Sa-Token 事件] 封禁：loginType={}, loginId={}, service={}, level={}, disableTime={}",
                loginType, loginId, service, level, disableTime);
    }

    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        log.info("[Sa-Token 事件] 解封：loginType={}, loginId={}, service={}", loginType, loginId, service);
    }

    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
        log.info("[Sa-Token 事件] 二级认证开启：loginType={}, service={}, safeTime={}", loginType, service, safeTime);
    }

    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
        log.info("[Sa-Token 事件] 二级认证关闭：loginType={}, service={}", loginType, service);
    }

    @Override
    public void doCreateSession(String id) {
        log.info("[Sa-Token 事件] 创建 Session：id={}", id);
    }

    @Override
    public void doLogoutSession(String id) {
        log.info("[Sa-Token 事件] 注销 Session：id={}", id);
    }

    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        log.info("[Sa-Token 事件] 续签：tokenValue={}, loginId={}, timeout={}", tokenValue, loginId, timeout);
    }
}

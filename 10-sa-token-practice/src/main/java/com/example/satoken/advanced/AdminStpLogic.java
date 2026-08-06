package com.example.satoken.advanced;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Admin 账号体系的 StpLogic。
 *
 * 直接 new 并注册到 SaManager，避免被 Spring 误当作默认 login 体系。
 */
public class AdminStpLogic extends StpLogic {

    public static final String LOGIN_TYPE = "admin";

    public AdminStpLogic() {
        super(LOGIN_TYPE);
    }
}

package com.example.satoken.advanced;

import cn.dev33.satoken.stp.StpLogic;

/**
 * 多账号体系：Admin 账号体系的 StpUtil。
 */
public class AdminStpUtil {

    private static final StpLogic stpLogic = new AdminStpLogic();

    public static StpLogic getStpLogic() {
        return stpLogic;
    }

    public static void login(Object id) {
        stpLogic.login(id);
    }

    public static void logout() {
        stpLogic.logout();
    }

    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    public static Object getLoginId() {
        return stpLogic.getLoginId();
    }
}

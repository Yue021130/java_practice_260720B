package com.example.tl.context;

import com.example.tl.context.dto.User;

/**
 * 用户上下文：每个请求线程独立持有。
 *
 * 面试八股：
 * - ThreadLocal 不是线程，而是线程的局部变量
 * - 底层是 Thread 对象里的 ThreadLocalMap，key 是 ThreadLocal 弱引用，value 是实际对象
 * - Web 场景下必须在请求结束时 remove，否则线程池复用会导致串号/内存泄漏
 */
public final class UserContext {

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

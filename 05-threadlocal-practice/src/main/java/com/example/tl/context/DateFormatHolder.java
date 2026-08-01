package com.example.tl.context;

import java.text.SimpleDateFormat;

/**
 * SimpleDateFormat 线程安全方案：每个线程一份实例。
 *
 * 面试八股：
 * - SimpleDateFormat 不是线程安全的，内部共享 calendar 等状态
 * - 高并发下会出现 parse/format 结果错乱、抛异常
 * - 解决方案：ThreadLocal、DateTimeFormatter（不可变线程安全）、同步锁
 */
public final class DateFormatHolder {

    private static final ThreadLocal<SimpleDateFormat> HOLDER = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    );

    private DateFormatHolder() {
    }

    public static SimpleDateFormat get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

package com.example.mp.extension;

/**
 * 动态表名上下文：保存当前线程的实际表名后缀。
 */
public final class TableNameContext {

    private static final ThreadLocal<String> SUFFIX = new ThreadLocal<>();

    private TableNameContext() {
    }

    public static void set(String suffix) {
        SUFFIX.set(suffix);
    }

    public static String get() {
        return SUFFIX.get();
    }

    public static void clear() {
        SUFFIX.remove();
    }
}

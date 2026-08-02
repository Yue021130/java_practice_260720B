package com.example.async.support;

/**
 * 模拟业务 traceId 上下文，用 ThreadLocal 保存在当前线程。
 */
public class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String get() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}

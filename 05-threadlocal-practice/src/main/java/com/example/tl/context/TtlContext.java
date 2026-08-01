package com.example.tl.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * TTL 上下文：演示线程池下的上下文透传。
 */
public final class TtlContext {

    private static final TransmittableThreadLocal<String> HOLDER = new TransmittableThreadLocal<>();

    private TtlContext() {
    }

    public static void set(String value) {
        HOLDER.set(value);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

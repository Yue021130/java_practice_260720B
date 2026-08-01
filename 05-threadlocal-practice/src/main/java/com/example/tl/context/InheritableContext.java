package com.example.tl.context;

/**
 * InheritableThreadLocal 上下文：父子线程值传递演示。
 */
public final class InheritableContext {

    private static final InheritableThreadLocal<String> HOLDER = new InheritableThreadLocal<>();

    private InheritableContext() {
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

package com.example.tlp.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路 ID 上下文：MDC 的最薄封装，全项目唯一入口。
 *
 * <p>MDC（Mapped Diagnostic Context）底层是 ThreadLocal&lt;Map&gt;，
 * 每个线程持有一份独立的上下文副本，日志 pattern 里用
 * {@code %X{traceId}} 取值——这就是「日志串联」的全部原理。</p>
 *
 * <p>为什么必须 finally clear()：Tomcat 处理请求的线程是复用的
 * （线程池），不清理 MDC，上一个请求的 traceId 会残留到下一个请求
 * ——这就是「线程池污染 / 串号」的来源。所有 set 的地方都必须成对
 * 出现在 try-finally 里。</p>
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Trace-Id";

    private TraceContext() {
    }

    /** 生成 32 位无横线 UUID，足够随机且 awk/grep 友好 */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String get() {
        return MDC.get(TRACE_ID);
    }

    public static void set(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }

    /**
     * 在「没有上游链路」的入口（定时任务、手动触发）开辟一条新链路，
     * 执行完恢复原值——嵌套调用安全。
     */
    public static void runWithNewTrace(Runnable runnable) {
        String old = get();
        set(generate());
        try {
            runnable.run();
        } finally {
            if (old != null) {
                set(old);
            } else {
                clear();
            }
        }
    }
}

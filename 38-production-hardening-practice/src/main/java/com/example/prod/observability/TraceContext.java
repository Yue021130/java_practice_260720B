package com.example.prod.observability;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC 链路上下文（第37章同款精简版）。
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Trace-Id";

    private TraceContext() {
    }

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
}

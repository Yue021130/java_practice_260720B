package com.example.async.support;

import org.springframework.core.task.TaskDecorator;

/**
 * TaskDecorator：把父线程的 traceId 复制到异步线程，并在任务执行后清理，
 * 解决 ThreadLocal 在线程池场景下的上下文透传问题。
 */
public class TraceIdTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        String traceId = TraceContext.get();
        return () -> {
            try {
                TraceContext.set(traceId);
                runnable.run();
            } finally {
                TraceContext.clear();
            }
        };
    }
}

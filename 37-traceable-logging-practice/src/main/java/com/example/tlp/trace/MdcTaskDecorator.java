package com.example.tlp.trace;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * MDC 透传装饰器：把提交任务时刻的 MDC 快照绑到异步线程上。
 *
 * <p>线程池里的线程和提交线程不是同一个 ThreadLocal 副本——不装饰
 * 的话异步任务里 MDC 是空的，日志 traceId 直接断链。原理：
 * 提交时 {@code getCopyOfContextMap()} 拷贝快照，任务在异步线程
 * 开头 {@code setContextMap} 恢复，结束 {@code clear} 防线程复用
 * 污染（和 TraceIdFilter 的 finally 同理，线程池里的线程同样复用）。</p>
 *
 * <p>Spring 的 ThreadPoolTaskExecutor 原生提供 setTaskDecorator 钩子；
 * 裸线程池（如 CompletableFuture 默认 ForkJoinPool）没有，要么自己
 * 在任务里包一层，要么用阿里 TTL（TransmittableThreadLocal，05 章
 * 已详讲）做自动透传。</p>
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

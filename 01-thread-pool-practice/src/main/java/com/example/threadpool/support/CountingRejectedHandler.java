package com.example.threadpool.support;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 带计数的自定义拒绝策略。
 *
 * 当线程池「队列已满 且 线程数已达 maxPoolSize」时，
 * ThreadPoolExecutor 会调用本策略处理放不进去的任务。
 *
 * 这里演示自定义策略的两个常见动作：
 * 1. 用 AtomicInteger 计数（多线程下必须线程安全，所以不用 int）；
 * 2. 打 WARN 日志，方便在控制台观察拒绝发生的时刻。
 *
 * 注意：JDK 自带的四种策略（AbortPolicy / CallerRunsPolicy /
 * DiscardPolicy / DiscardOldestPolicy）详见 README。
 */
@Slf4j
public class CountingRejectedHandler implements RejectedExecutionHandler {

    /** 累计被拒绝的任务数，供监控接口读取 */
    private final AtomicInteger rejectedCount = new AtomicInteger(0);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        int count = rejectedCount.incrementAndGet();
        // 打印当前池状态，帮助理解“为什么会被拒绝”
        log.warn("任务被拒绝！累计拒绝数={}，当前 poolSize={}，activeCount={}，queueSize={}",
                count, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());
    }

    /** 暴露计数给 PoolMetricsService */
    public int getRejectedCount() {
        return rejectedCount.get();
    }
}

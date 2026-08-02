package com.example.async.support;

import lombok.Getter;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 计数拒绝策略：每次拒绝时把计数器 +1，方便接口演示线程池打满后的拒绝量。
 */
public class CountingRejectedHandler implements RejectedExecutionHandler {

    @Getter
    private final AtomicLong rejectedCount = new AtomicLong(0);

    private final RejectedExecutionHandler delegate;

    public CountingRejectedHandler(RejectedExecutionHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        rejectedCount.incrementAndGet();
        delegate.rejectedExecution(r, executor);
    }
}

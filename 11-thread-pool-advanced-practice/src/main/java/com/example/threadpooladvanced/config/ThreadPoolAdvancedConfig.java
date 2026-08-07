package com.example.threadpooladvanced.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预定义线程池，用于与文章中的概念做对照实验。
 */
@Configuration
public class ThreadPoolAdvancedConfig {

    /**
     * CPU 密集型池：core == max，使用有界队列，适合计算任务。
     */
    @Bean("cpuPool")
    public ThreadPoolExecutor cpuPool() {
        int core = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                core, core,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(200),
                namedThreadFactory("cpu-pool"),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * IO 密集型池：core 较小、max 较大，队列较深，适合 IO 等待型任务。
     */
    @Bean("ioPool")
    public ThreadPoolExecutor ioPool() {
        int core = Runtime.getRuntime().availableProcessors() * 2;
        int max = Runtime.getRuntime().availableProcessors() * 4;
        return new ThreadPoolExecutor(
                core, max,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                namedThreadFactory("io-pool"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 小容量实验池：用于快速触发拒绝策略。
     */
    @Bean("tinyPool")
    public ThreadPoolExecutor tinyPool() {
        return new ThreadPoolExecutor(
                2, 4,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(5),
                namedThreadFactory("tiny-pool"),
                new CountingRejectedHandler()
        );
    }

    public static ThreadFactory namedThreadFactory(String prefix) {
        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
                t.setDaemon(false);
                return t;
            }
        };
    }

    /**
     * 自定义拒绝策略：计数 + 记录日志，便于实验观察。
     */
    public static class CountingRejectedHandler implements RejectedExecutionHandler {
        private final AtomicInteger rejectedCount = new AtomicInteger(0);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            rejectedCount.incrementAndGet();
            throw new RejectedExecutionException("任务被拒绝，当前拒绝计数: " + rejectedCount.get());
        }

        public int getRejectedCount() {
            return rejectedCount.get();
        }
    }
}

package com.example.threadpool.config;

import com.example.threadpool.support.CountingRejectedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池配置：定义三个定位不同的池，用于对比学习。
 *
 * 经验法则（面试高频）：
 * - CPU 密集型任务：线程数 ≈ CPU 核数（+1）。线程多了只会增加上下文切换，不会更快。
 * - IO 密集型任务：线程数 ≈ 2 * CPU 核数 起步（因为线程大量时间在等 IO，不占 CPU），
 *   更精细的公式：核数 * (1 + 平均等待时间 / 平均计算时间)。
 * - 队列不要用无界队列（默认 Integer.MAX_VALUE），否则 maxPoolSize 永远不生效，
 *   任务无限堆积最终 OOM——这是生产事故经典来源。
 */
@Configuration
public class ThreadPoolConfig {

    /** 当前机器可用 CPU 核数，很多参数都基于它推算 */
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * 拒绝策略 Bean：单独注册，供 customPool 使用。
     * 只有 customPool 使用它；其他两个池用默认 AbortPolicy（直接抛异常）。
     * 监控侧通过 executor.getRejectedExecutionHandler() 反查该实例读取计数。
     */
    @Bean
    public CountingRejectedHandler customRejectedHandler() {
        return new CountingRejectedHandler();
    }

    /**
     * cpuPool：演示 CPU 密集型推荐配置。
     *
     * corePoolSize = maxPoolSize = CPU 核数：
     * - CPU 密集型任务全程占用 CPU，线程数超过核数只会徒增切换开销；
     * - core == max 表示不扩容，超出能力的任务进队列排队。
     *
     * 队列给 200 而非无界：让“打满”这件事在前端面板上看得见。
     * 拒绝策略用默认 AbortPolicy：直接抛 RejectedExecutionException，
     * 由提交方感知（演示“默认策略会抛异常”这一点）。
     */
    @Bean("cpuPool")
    public ThreadPoolTaskExecutor cpuPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES);
        executor.setMaxPoolSize(CPU_CORES);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        // 线程名前缀：jstack / 日志里一眼认出是哪个池的线程
        executor.setThreadNamePrefix("cpu-pool-");
        // 关闭应用时等待已提交任务执行完，避免任务被腰斩
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * ioPool：演示 IO 密集型配置。
     *
     * core = 2 * 核数，max = 4 * 核数：
     * - IO 任务大量时间在 sleep/等网络/等磁盘，线程多数时间不占 CPU，
     *   所以可以多配线程提高吞吐；
     * - max > core，队列（500）打满后允许临时扩容应对突发流量，
     *   空闲超过 keepAliveSeconds 的非核心线程会被回收。
     */
    @Bean("ioPool")
    public ThreadPoolTaskExecutor ioPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2 * CPU_CORES);
        executor.setMaxPoolSize(4 * CPU_CORES);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("io-pool-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * customPool：演示「打满队列 → 触发拒绝策略」的完整链路。
     *
     * 故意配得很小：core=2，max=4，队列容量=5。
     * 即最多同时容纳 4（运行中）+ 5（排队）= 9 个任务，
     * 第 10 个及以后就会触发 CountingRejectedHandler。
     * 向前端一次性提交 20 个耗时任务，就能在面板上看到拒绝数上涨。
     */
    @Bean("customPool")
    public ThreadPoolTaskExecutor customPool(CountingRejectedHandler customRejectedHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(5);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("custom-pool-");
        // 使用自定义拒绝策略：计数 + 日志，而不是抛异常
        executor.setRejectedExecutionHandler(customRejectedHandler);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

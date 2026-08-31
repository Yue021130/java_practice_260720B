package com.example.ae.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 *
 * <p>八股要点：</p>
 * <ul>
 *     <li>corePoolSize：核心线程数，即使空闲也保留。</li>
 *     <li>maxPoolSize：最大线程数，队列满后才会创建非核心线程。</li>
 *     <li>queueCapacity：任务队列容量，LinkedBlockingQueue。</li>
 *     <li>rejection-policy：CallerRunsPolicy 让调用线程自己执行，避免丢任务。</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.executor.core-pool-size:4}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${async.executor.thread-name-prefix:async-task-}")
    private String threadNamePrefix;

    /**
     * 业务异步线程池，@Async("taskExecutor") 使用。
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        // 拒绝策略：调用者线程执行，保证不丢任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("[AsyncConfig] 异步线程池初始化完成：core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }
}

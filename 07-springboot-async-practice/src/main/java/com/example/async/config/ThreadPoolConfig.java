package com.example.async.config;

import com.example.async.support.CountingRejectedHandler;
import com.example.async.support.TraceIdTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 多业务线程池配置。
 * <p>每个池都开启优雅关闭：waitForTasksToCompleteOnShutdown + awaitTerminationSeconds，
 * 并统一设置 TaskDecorator 做 ThreadLocal 上下文透传。
 */
@Configuration
public class ThreadPoolConfig {

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    @Bean
    @Primary
    public ThreadPoolTaskExecutor defaultPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES);
        executor.setMaxPoolSize(CPU_CORES * 2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("default-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new TraceIdTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor cpuPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES);
        executor.setMaxPoolSize(CPU_CORES);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cpu-pool-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new TraceIdTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor ioPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CPU_CORES * 2);
        executor.setMaxPoolSize(CPU_CORES * 4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("io-pool-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new TraceIdTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor rejectDemoPool() {
        CountingRejectedHandler handler = new CountingRejectedHandler(new ThreadPoolExecutor.AbortPolicy());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("reject-demo-");
        executor.setRejectedExecutionHandler(handler);
        executor.setTaskDecorator(new TraceIdTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

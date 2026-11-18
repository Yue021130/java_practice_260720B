package com.example.sep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Async 专用线程池。
 *
 * <p>坑：不显式配置时，@Async 用的是 Spring 默认的 SimpleAsyncTaskExecutor
 * ——它根本不池化，每次调用 new 一个新线程！高并发下直接线程爆炸。
 * 还有第二个坑：@Async 指定了不存在的线程池 bean 名，部分版本会
 * 静默降级到默认执行器。所以生产必须显式声明并指定。</p>
 *
 * <p>事件监听是「写通知/写日志」这类 IO 型任务，核心线程数给足、
 * 队列要有界（无界队列会掩盖消费堆积直到 OOM）、拒绝策略用
 * CallerRunsPolicy 让发布线程兜底执行，宁可慢不能丢。</p>
 */
@Configuration
public class AsyncConfig {

    @Bean("eventExecutor")
    public ThreadPoolTaskExecutor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("event-async-");
        // 队列满 + 线程满时，提交线程自己跑任务：慢，但不丢事件
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

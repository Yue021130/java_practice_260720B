package com.example.tlp.config;

import com.example.tlp.trace.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 业务线程池：批次任务、@Async、CompletableFuture 共用。
 *
 * <p>三个必须显式配置的理由（对应八股）：</p>
 * <ol>
 *   <li>默认 SimpleAsyncTaskExecutor 不池化，每次 new 线程，高并发
 *   直接线程爆炸；</li>
 *   <li>必须 setTaskDecorator(new MdcTaskDecorator())，否则异步任务
 *   日志断链；</li>
 *   <li>队列必须有界 + 拒绝策略兜底：CallerRunsPolicy 让提交线程
 *   自己跑，慢但不丢任务。</li>
 * </ol>
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean("batchExecutor")
    public ThreadPoolTaskExecutor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("batch-");
        // 【关键】MDC 透传，缺了它异步日志 traceId 断链
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：停机时等任务跑完再销毁，避免任务半路被杀
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

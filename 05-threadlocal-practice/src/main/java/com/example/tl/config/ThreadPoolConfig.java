package com.example.tl.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 演示用线程池配置。
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 普通固定线程池：用于演示线程池复用导致的 ThreadLocal 污染。
     */
    @Bean(name = "demoExecutor")
    public ExecutorService demoExecutor() {
        return Executors.newFixedThreadPool(1);
    }

    /**
     * 包装后的 TTL 线程池：用于演示跨线程池上下文透传。
     */
    @Bean(name = "ttlExecutor")
    public ExecutorService ttlExecutor() {
        return TtlExecutors.getTtlExecutorService(Executors.newFixedThreadPool(1));
    }
}

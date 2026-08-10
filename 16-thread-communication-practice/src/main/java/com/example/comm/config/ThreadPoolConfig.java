package com.example.comm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 演示线程池基础设施配置。
 *
 * 各场景需要「真实跑出并发效果」的多线程演示（wait-notify / 同步工具 / 阻塞队列 /
 * CompletableFuture 等），统一注入这里的线程池，避免每个场景各自 new 线程、
 * 也难以回收。两个池子分工：
 * - fixedPool：有界、可控，适合「N 个工人协作」类场景（join / latch / barrier / semaphore）；
 * - cachedPool：无限伸缩，适合「任务不固定、来一个跑一个」的异步编排（CompletableFuture）。
 *
 * 容器销毁时统一关闭，避免非 daemon 线程阻止 JVM 退出。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfig {

    private final CommPracticeProperties props;

    /**
     * 固定大小线程池：核心 = 最大 = coreSize，满了进有界队列。
     * 用「有界 + 拒绝后由调用方兜底」的真实 ThreadPoolExecutor，方便讲线程池语义。
     */
    @Bean("fixedPool")
    public ExecutorService fixedPool() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                props.getPool().getCoreSize(),
                props.getPool().getMaxSize(),
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(props.getPool().getQueueCapacity()),
                r -> new Thread(r, "comm-fixed-" + r.hashCode()));
        pool.allowCoreThreadTimeOut(true);
        log.info("fixedPool 就绪：core={}, max={}, queue={}",
                props.getPool().getCoreSize(), props.getPool().getMaxSize(),
                props.getPool().getQueueCapacity());
        return pool;
    }

    /**
     * 缓存线程池：按需创建、空闲 60s 回收，适合异步编排。
     */
    @Bean("cachedPool")
    public ExecutorService cachedPool() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "comm-cached-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭演示线程池...");
    }
}

package com.example.threadpool.service;

import com.example.threadpool.dto.PoolMetricsDto;
import com.example.threadpool.support.CountingRejectedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控指标服务。
 *
 * 关键点：ThreadPoolTaskExecutor 是 Spring 对原生 ThreadPoolExecutor 的封装，
 * 读运行期指标要通过 getThreadPoolExecutor() 拿到原生对象。
 */
@Service
@RequiredArgsConstructor
public class PoolMetricsService {

    /** 所有线程池 Bean，key 为 Bean 名（cpuPool / ioPool / customPool） */
    private final Map<String, ThreadPoolTaskExecutor> poolMap;

    /**
     * 动态调整线程池参数。
     *
     * ThreadPoolExecutor 的 corePoolSize / maxPoolSize 支持运行期修改，
     * 这正是“线程池参数动态化”（如配合配置中心）的底层能力。
     *
     * 注意设置顺序：ThreadPoolExecutor 要求 corePoolSize <= maxPoolSize，
     * 因此调大时先设 max 再设 core，调小时先设 core 再设 max，
     * 否则会抛 IllegalArgumentException。
     */
    public void resize(String poolName, int corePoolSize, int maxPoolSize) {
        ThreadPoolTaskExecutor executor = poolMap.get(poolName);
        if (executor == null) {
            throw new IllegalArgumentException(
                    "线程池不存在：" + poolName + "，可选值：" + poolMap.keySet());
        }
        if (corePoolSize > executor.getMaxPoolSize()) {
            // 调大场景：新 core 可能超过旧 max，必须先抬 max
            executor.setMaxPoolSize(maxPoolSize);
            executor.setCorePoolSize(corePoolSize);
        } else {
            // 调小场景：先降 core，再降 max
            executor.setCorePoolSize(corePoolSize);
            executor.setMaxPoolSize(maxPoolSize);
        }
    }

    /** 采集所有线程池的实时指标，按池名排序返回 */
    public List<PoolMetricsDto> getAllMetrics() {
        List<PoolMetricsDto> result = new ArrayList<>();
        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : poolMap.entrySet()) {
            result.add(buildMetrics(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparing(PoolMetricsDto::getPoolName));
        return result;
    }

    /** 从原生 ThreadPoolExecutor 读取单个池的指标 */
    private PoolMetricsDto buildMetrics(String poolName, ThreadPoolTaskExecutor taskExecutor) {
        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
        BlockingQueue<Runnable> queue = executor.getQueue();
        // 队列容量 = 剩余容量 + 已占用。对无界队列来说剩余容量是 Integer.MAX_VALUE。
        int queueCapacity = queue.size() + queue.remainingCapacity();

        // 拒绝计数：直接从执行器当前生效的拒绝策略反查，
        // 是 CountingRejectedHandler 就读计数，否则（默认 AbortPolicy）记 0。
        int rejectedCount = 0;
        if (executor.getRejectedExecutionHandler() instanceof CountingRejectedHandler) {
            rejectedCount = ((CountingRejectedHandler) executor.getRejectedExecutionHandler()).getRejectedCount();
        }

        return PoolMetricsDto.builder()
                .poolName(poolName)
                .corePoolSize(executor.getCorePoolSize())
                .maxPoolSize(executor.getMaximumPoolSize())
                .activeCount(executor.getActiveCount())
                .poolSize(executor.getPoolSize())
                .queueSize(queue.size())
                .queueCapacity(queueCapacity)
                .completedTaskCount(executor.getCompletedTaskCount())
                .rejectedCount(rejectedCount)
                .build();
    }
}

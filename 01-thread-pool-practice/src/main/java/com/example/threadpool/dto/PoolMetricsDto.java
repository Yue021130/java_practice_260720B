package com.example.threadpool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 单个线程池的实时监控指标。
 *
 * 数据来源：ThreadPoolTaskExecutor#getThreadPoolExecutor() 拿到原生
 * ThreadPoolExecutor 后读取，再加自定义的拒绝计数。
 */
@Data
@Builder
@Schema(description = "单个线程池的实时监控指标")
public class PoolMetricsDto {

    /** 池名（即 Spring Bean 名）：cpuPool / ioPool / customPool */
    @Schema(description = "池名（即 Spring Bean 名）", example = "customPool")
    private String poolName;

    /** 核心线程数：池中长期保留的线程数（默认即使空闲也不回收） */
    @Schema(description = "核心线程数", example = "2")
    private Integer corePoolSize;

    /** 最大线程数：队列满后允许扩容到的线程上限 */
    @Schema(description = "最大线程数", example = "4")
    private Integer maxPoolSize;

    /** 当前正在执行任务的活跃线程数 */
    @Schema(description = "当前正在执行任务的活跃线程数", example = "4")
    private Integer activeCount;

    /** 当前池中实际存在的线程总数 */
    @Schema(description = "当前池中实际存在的线程总数", example = "4")
    private Integer poolSize;

    /** 队列中排队等待执行的任务数（积压量） */
    @Schema(description = "队列中排队等待的任务数", example = "5")
    private Integer queueSize;

    /** 队列总容量；Integer.MAX_VALUE 基本等于无界队列 */
    @Schema(description = "队列总容量", example = "5")
    private Integer queueCapacity;

    /** 已完成任务总数（近似值，线程池滚动累计） */
    @Schema(description = "已完成任务总数（近似值）", example = "42")
    private Long completedTaskCount;

    /** 被自定义拒绝策略 CountingRejectedHandler 拒绝的任务数 */
    @Schema(description = "被自定义拒绝策略拒绝的任务数", example = "3")
    private Integer rejectedCount;
}

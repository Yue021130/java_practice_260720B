package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * 线程池实时指标。
 */
@Data
public class PoolMetricsDto {

    private String poolId;
    private String poolName;
    private int corePoolSize;
    private int maximumPoolSize;
    private int poolSize;
    private int activeCount;
    private int queueSize;
    private int queueRemainingCapacity;
    private long completedTaskCount;
    private long taskCount;
    private long keepAliveTimeSeconds;
    private boolean shutdown;
    private boolean terminated;
    private Integer rejectedCount;
}

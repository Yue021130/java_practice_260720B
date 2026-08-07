package com.example.threadpooladvanced.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 创建自定义线程池请求。
 */
@Data
public class CustomPoolRequest {

    @NotBlank
    private String poolId;

    @Min(0)
    private int corePoolSize;

    @Min(1)
    private int maximumPoolSize;

    @Min(0)
    private long keepAliveTime;

    private String timeUnit = "SECONDS";

    @Min(0)
    private int queueCapacity;

    private String queueType = "ArrayBlockingQueue";

    private String rejectionPolicy = "AbortPolicy";

    private String threadFactoryPrefix = "custom";
}

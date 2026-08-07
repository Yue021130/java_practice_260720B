package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * 线程池生命周期状态。
 */
@Data
public class LifecycleStateDto {

    private String state;
    private int stateCode;
    private String description;
    private String acceptNewTasks;
    private String processQueueTasks;
    private String interruptWorkers;
}

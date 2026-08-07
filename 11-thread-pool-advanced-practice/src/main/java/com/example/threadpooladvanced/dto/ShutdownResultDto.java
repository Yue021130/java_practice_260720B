package com.example.threadpooladvanced.dto;

import lombok.Data;

import java.util.List;

/**
 * 关闭线程池结果。
 */
@Data
public class ShutdownResultDto {

    private String poolId;
    private boolean shutdown;
    private boolean terminated;
    private List<String> pendingTasks;
    private String message;
}

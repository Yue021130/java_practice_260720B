package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * ThreadPoolExecutor.execute() 流程步骤。
 */
@Data
public class SourceFlowStepDto {

    private int step;
    private String title;
    private String description;
    private String keyMethod;
}

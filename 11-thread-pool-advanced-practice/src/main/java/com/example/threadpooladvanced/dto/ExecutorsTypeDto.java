package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * Executors 工厂方法说明。
 */
@Data
public class ExecutorsTypeDto {

    private String type;
    private String methodCall;
    private String corePoolSize;
    private String maximumPoolSize;
    private String queue;
    private String feature;
    private String useCase;
    private String risk;
}

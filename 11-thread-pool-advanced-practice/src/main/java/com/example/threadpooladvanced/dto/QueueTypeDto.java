package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * 阻塞队列类型说明。
 */
@Data
public class QueueTypeDto {

    private String type;
    private String underlying;
    private boolean bounded;
    private String defaultCapacity;
    private String feature;
    private String useCase;
    private String risk;
}

package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * 阻塞队列实验结果。
 */
@Data
public class QueueExperimentResult {

    private String queueType;
    private int submitted;
    private int accepted;
    private int rejected;
    private String note;
}

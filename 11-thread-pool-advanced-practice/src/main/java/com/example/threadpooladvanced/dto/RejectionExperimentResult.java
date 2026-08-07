package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * 拒绝策略实验结果。
 */
@Data
public class RejectionExperimentResult {

    private String policy;
    private int submitted;
    private int executed;
    private int rejected;
    private String behavior;
    private String message;
}

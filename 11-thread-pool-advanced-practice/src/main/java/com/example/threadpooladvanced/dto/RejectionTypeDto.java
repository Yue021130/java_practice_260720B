package com.example.threadpooladvanced.dto;

import lombok.Data;

/**
 * 拒绝策略说明。
 */
@Data
public class RejectionTypeDto {

    private String policy;
    private String behavior;
    private String useCase;
    private String risk;
}

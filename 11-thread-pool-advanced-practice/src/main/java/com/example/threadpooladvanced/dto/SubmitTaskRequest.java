package com.example.threadpooladvanced.dto;

import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 批量提交任务请求。
 */
@Data
public class SubmitTaskRequest {

    @Min(1)
    private int count = 1;

    @Min(0)
    private long taskDurationMs = 500;
}

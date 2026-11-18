package com.example.sep.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志出参。
 */
@Data
public class AuditLogVO {

    private Long id;

    private String action;

    private String detail;

    private String operator;

    private LocalDateTime createTime;
}

package com.example.sep.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假单出参。
 */
@Data
public class LeaveVO {

    private Long id;

    private String username;

    private String type;

    private String reason;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 审批状态：0 待审批，1 已通过，2 已拒绝 */
    private Integer status;

    private String approver;

    private LocalDateTime createTime;
}

package com.example.sep.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 请假审批入参：approve / reject 两个接口共用，
 * approved 由接口语义决定（/approve 恒 true、/reject 恒 false），body 只需传 id + approver。
 */
@Data
public class LeaveApproveDTO {

    /** 请假单 id */
    @NotNull(message = "请假单 id 不能为空")
    private Long id;

    /** 是否通过：true 通过，false 拒绝（不传时由接口语义决定） */
    private Boolean approved;

    /** 审批人 */
    private String approver;

    /** 审批意见（可空） */
    private String reason;
}

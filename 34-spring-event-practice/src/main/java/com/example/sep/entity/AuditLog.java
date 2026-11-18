package com.example.sep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体，对应表 biz_audit_log。
 *
 * <p>由监听器在事务提交后写入，记录关键业务动作，便于事后审计。</p>
 */
@Data
@TableName("biz_audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 动作（如 LEAVE_SUBMIT / LEAVE_APPROVE） */
    private String action;

    /** 动作明细（JSON 或描述文本） */
    private String detail;

    /** 操作人 */
    private String operator;

    private LocalDateTime createTime;
}

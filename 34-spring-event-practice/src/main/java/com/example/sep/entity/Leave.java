package com.example.sep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假单实体，对应表 biz_leave。
 */
@Data
@TableName("biz_leave")
public class Leave {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 请假人用户名 */
    private String username;

    /** 请假类型：SICK 病假 / CASUAL 事假 / ANNUAL 年假 */
    private String type;

    /** 请假事由 */
    private String reason;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 审批状态：0 待审批，1 已通过，2 已拒绝 */
    private Integer status;

    /** 审批人 */
    private String approver;

    /** 逻辑删除：0 未删，1 已删 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

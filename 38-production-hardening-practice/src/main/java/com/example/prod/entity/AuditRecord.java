package com.example.prod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计记录：对应 Spring Boot Actuator 的 audit_event 概念。
 *
 * <p>登录成功/登出/订单增删改都会发一条 AuditEvent；
 * 由并行开发的审计落库组件（AuditEventRepository 的持久化实现）写入本表，
 * 前端「加固看板」通过 /api/audit/page 分页查询。</p>
 *
 * <p>timestamp 选 LocalDateTime 而非 Instant：与 sys_user/biz_order 的 create_time
 * 保持一致（MySQL DATETIME + serverTimezone=Asia/Shanghai），
 * 且 MyBatis-Plus 对 LocalDateTime 的 TypeHandler 开箱即用，无需额外注册。</p>
 */
@Data
@TableName("audit_event")
public class AuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作主体（用户名） */
    private String principal;

    /** 事件类型：LOGIN_SUCCESS / LOGOUT / ORDER_CREATE / ORDER_UPDATE / ORDER_DELETE */
    private String type;

    /** 事件上下文，JSON/键值文本（如 ip、orderNo） */
    private String data;

    /** 事件发生时间（LocalDateTime，见类注释） */
    private LocalDateTime timestamp;
}

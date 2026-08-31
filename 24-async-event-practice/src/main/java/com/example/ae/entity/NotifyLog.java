package com.example.ae.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知日志：记录短信、邮件、积分等异步处理结果。
 */
@Data
@Entity
@Table(name = "t_notify_log")
public class NotifyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单号。 */
    @Column(length = 64)
    private String orderNo;

    /** 通知类型：SMS/EMAIL/POINTS。 */
    @Column(length = 32)
    private String notifyType;

    /** 处理线程名。 */
    @Column(length = 128)
    private String threadName;

    /** 处理结果描述。 */
    @Column(length = 256)
    private String result;

    /** 创建时间。 */
    private LocalDateTime createTime;
}

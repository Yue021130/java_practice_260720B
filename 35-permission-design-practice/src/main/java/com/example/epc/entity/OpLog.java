package com.example.epc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志（审计）：@OpLog 注解 + AOP 自动记录，见 oplog/OpLogAspect。
 */
@Data
@TableName("sys_op_log")
public class OpLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作标题，来自 @OpLog.value() */
    private String title;

    /** 关联权限点标识，来自 @OpLog.perm() */
    private String perm;

    /** 操作人用户名 */
    private String operator;

    /** 方法签名（类全名#方法名） */
    private String method;

    /** 请求 IP */
    private String ip;

    /** 参数摘要（JSON，超长截断） */
    private String params;

    /** 耗时毫秒 */
    private Long costMs;

    private LocalDateTime createTime;
}

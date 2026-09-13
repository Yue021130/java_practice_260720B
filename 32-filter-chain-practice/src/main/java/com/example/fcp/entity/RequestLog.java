package com.example.fcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请求审计日志实体，对应表 sys_request_log：由 TimingFilter(OncePerRequestFilter) 自动写入。
 *
 * <p>与第31章 sys_api_log 的区别：位于 Filter 层，位于鉴权 Filter 之前，
 * 因此包含鉴权失败的 401 请求；带 traceId 可与全链路日志关联。</p>
 */
@Data
@TableName("sys_request_log")
public class RequestLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 全链路追踪 ID（TraceIdFilter 生成） */
    private String traceId;

    /** 客户端 IP */
    private String ip;

    /** HTTP 方法 */
    private String method;

    /** 请求 URI */
    private String uri;

    /** 响应状态码 */
    private Integer statusCode;

    /** 耗时毫秒 */
    private Integer costMs;

    /** 是否异常：0 否，1 是 */
    private Integer hasError;

    private LocalDateTime createTime;
}

package com.example.sfp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口审计日志实体，对应表 sys_api_log：由 LogInterceptor 自动写入。
 */
@Data
@TableName("sys_api_log")
public class ApiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户端 IP */
    private String ip;

    /** HTTP 方法 */
    private String method;

    /** 请求 URI */
    private String uri;

    /** User-Agent */
    private String userAgent;

    /** 登录用户名（未登录为 anonymous） */
    private String username;

    /** 响应状态码 */
    private Integer statusCode;

    /** 耗时毫秒 */
    private Integer costMs;

    /** 是否异常：0 否，1 是 */
    private Integer hasError;

    private LocalDateTime createTime;
}

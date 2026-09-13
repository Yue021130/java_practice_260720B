package com.example.fcp.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 请求审计日志出参 VO。
 */
@Data
public class RequestLogVO implements Serializable {

    private Long id;

    private String traceId;

    private String ip;

    private String method;

    private String uri;

    private Integer statusCode;

    private Integer costMs;

    private Integer hasError;

    private LocalDateTime createTime;
}

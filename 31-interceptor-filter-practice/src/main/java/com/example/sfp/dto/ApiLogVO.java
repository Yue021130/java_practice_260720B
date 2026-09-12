package com.example.sfp.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 接口审计日志出参。
 */
@Data
public class ApiLogVO implements Serializable {

    private Long id;

    private String ip;

    private String method;

    private String uri;

    private String userAgent;

    private String username;

    private Integer statusCode;

    private Integer costMs;

    private Integer hasError;

    private LocalDateTime createTime;
}

package com.example.fcp.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告出参 VO。
 */
@Data
public class NoticeVO implements Serializable {

    private Long id;

    private String title;

    private Integer type;

    private Integer status;

    /** 正文（已持久化，展示时按原样返回） */
    private String content;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

package com.example.sep.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知出参。
 */
@Data
public class NotifyVO {

    private Long id;

    private String receiver;

    private String title;

    private String content;

    /** 是否已读：0 未读，1 已读 */
    private Integer readFlag;

    private LocalDateTime createTime;
}

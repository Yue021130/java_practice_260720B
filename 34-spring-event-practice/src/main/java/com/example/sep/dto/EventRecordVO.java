package com.example.sep.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件记录出参：携带已消费的监听器名列表，方便排查事件去向。
 */
@Data
public class EventRecordVO {

    private Long id;

    private String eventType;

    private String payload;

    /** 消费状态：0 待消费，1 已消费 */
    private Integer status;

    private LocalDateTime createTime;

    /** 处理过该事件的监听器名列表 */
    private List<String> handledBy;
}

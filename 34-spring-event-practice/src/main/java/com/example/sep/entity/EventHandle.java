package com.example.sep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件消费明细实体，对应表 event_handle。
 *
 * <p>记录某个事件被哪个监听器处理过，便于排查"事件发了但通知没到"的问题。</p>
 */
@Data
@TableName("event_handle")
public class EventHandle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联事件记录 id（event_record.id） */
    private Long eventId;

    /** 处理该事件的监听器名称（类名） */
    private String listenerName;

    private LocalDateTime createTime;
}

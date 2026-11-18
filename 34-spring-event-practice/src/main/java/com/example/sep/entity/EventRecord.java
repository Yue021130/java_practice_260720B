package com.example.sep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件记录实体，对应表 event_record（本地消息表思想）。
 *
 * <p>事件先落库再广播：即使监听器消费失败，事件也有据可查、可补偿；
 * status 标记是否已被消费完毕。</p>
 */
@Data
@TableName("event_record")
public class EventRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件类型（如 LeaveSubmittedEvent） */
    private String eventType;

    /** 事件负载（JSON 字符串） */
    private String payload;

    /** 消费状态：0 待消费，1 已消费 */
    private Integer status;

    private LocalDateTime createTime;
}

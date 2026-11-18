package com.example.sep.event;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 请假提交事件——普通 POJO，不继承 ApplicationEvent。
 *
 * <p>Spring 4.2 之前事件必须继承 ApplicationEvent；4.2 之后
 * publishEvent(Object) 支持任意对象，事件类可以是不受框架约束的
 * 纯业务 POJO，这也是现在推荐的写法（继承 ApplicationEvent
 * 会让领域对象携带框架依赖，且事件源强耦合 Spring）。</p>
 *
 * <p>eventId 不是事件本身的业务属性，而是「本地消息表」实现需要：
 * 发布前先把事件落 event_record 表拿到主键，广播时挂在事件上，
 * 各监听器消费后按 eventId 回写 event_handle 明细，形成完整链路。</p>
 */
@Getter
@ToString
public class LeaveSubmittedEvent {

    /** event_record 表主键，发布前落表后设置 */
    @Setter
    private Long eventId;

    private final Long leaveId;
    private final String username;
    /** SICK 病假 / CASUAL 事假 / ANNUAL 年假 */
    private final String type;
    private final String reason;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public LeaveSubmittedEvent(Long leaveId, String username, String type,
                               String reason, LocalDate startDate, LocalDate endDate) {
        this.leaveId = leaveId;
        this.username = username;
        this.type = type;
        this.reason = reason;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}

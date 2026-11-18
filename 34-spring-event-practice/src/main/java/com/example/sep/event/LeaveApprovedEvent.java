package com.example.sep.event;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 请假审批结果事件——普通 POJO 事件（同 {@link LeaveSubmittedEvent} 的设计说明）。
 *
 * <p>由 LeaveService 的事务方法在更新单据状态后发布，
 * 监听方是 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}：
 * 只有事务真正提交后通知监听器才执行，保证「通知里看到的单据状态
 * 一定是已提交的新状态」。这正是「事务事件」解决的核心坑——
 * 如果监听器在事务提交前异步读库，可能读到旧状态甚至因隔离级别
 * 读不到刚插入的数据。</p>
 */
@Getter
@ToString
public class LeaveApprovedEvent {

    /** event_record 表主键，发布前落表后设置 */
    @Setter
    private Long eventId;

    private final Long leaveId;
    private final String username;
    /** SICK 病假 / CASUAL 事假 / ANNUAL 年假 */
    private final String type;
    /** true 通过 / false 拒绝 */
    private final boolean approved;
    private final String approver;

    public LeaveApprovedEvent(Long leaveId, String username, String type,
                              boolean approved, String approver) {
        this.leaveId = leaveId;
        this.username = username;
        this.type = type;
        this.approved = approved;
        this.approver = approver;
    }
}

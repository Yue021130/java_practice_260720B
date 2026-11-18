package com.example.sep.listener;

import com.example.sep.event.LeaveApprovedEvent;
import com.example.sep.service.EventRecordService;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 统计监听器：同步监听「审批结果」事件，累加内存计数器。
 *
 * <p>与 {@link NotifyListener} 对比：同一个 LeaveApprovedEvent，
 * 本监听器是普通 @EventListener（事务内同步执行，用于刷新进程内
 * 统计口径），NotifyListener 是 AFTER_COMMIT + @Async（事务后异步
 * 执行，用于外部可见动作）。一个事件多种订阅方式并存是常态。</p>
 *
 * <p>{@code @Order(1)}：同一事件有多个同步监听器时，按 @Order 值
 * 从小到大顺序执行，缺省值是 Ordered.LOWEST_PRECEDENCE（最后）。
 * 这里的计数必须在 NotifyListener 之前完成纯属演示语义，实际业务
 * 中常用于「先校验后动作」编排。</p>
 */
@Component
public class StatsListener {

    private final EventRecordService eventRecordService;
    /** 进程内审批通过总数（重启清零，演示用；生产应落库或走 Redis） */
    private final AtomicLong approvedCount = new AtomicLong();

    public StatsListener(EventRecordService eventRecordService) {
        this.eventRecordService = eventRecordService;
    }

    @Order(1)
    @EventListener
    public void handle(LeaveApprovedEvent event) {
        if (event.isApproved()) {
            approvedCount.incrementAndGet();
        }
        eventRecordService.markHandled(event.getEventId(), "StatsListener");
    }

    public long getApprovedCount() {
        return approvedCount.get();
    }

    /** 测试用重置 */
    public void reset() {
        approvedCount.set(0);
    }
}

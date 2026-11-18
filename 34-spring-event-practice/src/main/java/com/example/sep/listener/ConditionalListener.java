package com.example.sep.listener;

import com.example.sep.entity.AuditLog;
import com.example.sep.event.LeaveSubmittedEvent;
import com.example.sep.mapper.AuditLogMapper;
import com.example.sep.service.EventRecordService;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 条件监听器：只对「年假」提交的请假单做额外登记。
 *
 * <p>{@code condition = "#event.type == 'ANNUAL'"} 是 SpEL 表达式，
 * 在事件投递前求值，false 直接跳过——比监听器里写 if 更声明式，
 * 也比为每种类型建一个事件类更轻量。事件对象在表达式里固定叫
 * {@code #event}（@TransactionalEventListener 同样支持 condition）。</p>
 *
 * <p>{@code @Order(2)}：与 AuditLogListener（@Order(1)）监听同一个
 * LeaveSubmittedEvent，确定性顺序：先记提交审计，再记年假台账。</p>
 */
@Component
public class ConditionalListener {

    private final AuditLogMapper auditLogMapper;
    private final EventRecordService eventRecordService;

    public ConditionalListener(AuditLogMapper auditLogMapper,
                               EventRecordService eventRecordService) {
        this.auditLogMapper = auditLogMapper;
        this.eventRecordService = eventRecordService;
    }

    @Order(2)
    @EventListener(condition = "#event.type == 'ANNUAL'")
    public void handle(LeaveSubmittedEvent event) {
        AuditLog log = new AuditLog();
        log.setAction("ANNUAL_LEAVE_REGISTER");
        log.setDetail(String.format("年假台账登记：用户[%s] %s ~ %s，单号 %d",
                event.getUsername(), event.getStartDate(), event.getEndDate(),
                event.getLeaveId()));
        log.setOperator("system");
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);

        eventRecordService.markHandled(event.getEventId(), "ConditionalListener");
    }
}

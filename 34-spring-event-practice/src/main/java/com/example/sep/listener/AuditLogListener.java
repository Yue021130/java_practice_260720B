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
 * 审计监听器：同步监听「请假提交」事件，写审计日志。
 *
 * <p>为什么用同步（默认）而不是 @Async：审计要求「响应返回前审计
 * 一定落库」，如果异步，接口返回时审计可能还没写——出事后翻不到记录。
 * 同步的代价是发布线程被拖慢，这里审计只是插一条库，可接受。</p>
 *
 * <p>坑演示：同步监听器抛出的异常会传播给 publishEvent 的调用方。
 * 如果 LeaveService.submit 在事务里且监听器抛异常，事务会回滚——
 * 这是「事件监听把主流程搞挂」的经典坑，监听器必须自己 try-catch
 * 非关键异常，或改用异步。</p>
 */
@Component
public class AuditLogListener {

    private final AuditLogMapper auditLogMapper;
    private final EventRecordService eventRecordService;

    public AuditLogListener(AuditLogMapper auditLogMapper,
                            EventRecordService eventRecordService) {
        this.auditLogMapper = auditLogMapper;
        this.eventRecordService = eventRecordService;
    }

    @Order(1)
    @EventListener
    public void handle(LeaveSubmittedEvent event) {
        AuditLog log = new AuditLog();
        log.setAction("LEAVE_SUBMIT");
        log.setDetail(String.format("用户[%s]提交了[%s]请假：%s（%s ~ %s），单号 %d",
                event.getUsername(), event.getType(), event.getReason(),
                event.getStartDate(), event.getEndDate(), event.getLeaveId()));
        log.setOperator(event.getUsername());
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);

        eventRecordService.markHandled(event.getEventId(), "AuditLogListener");
    }
}

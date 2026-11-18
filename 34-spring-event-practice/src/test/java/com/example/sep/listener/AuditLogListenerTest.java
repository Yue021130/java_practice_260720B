package com.example.sep.listener;

import com.example.sep.entity.AuditLog;
import com.example.sep.event.LeaveSubmittedEvent;
import com.example.sep.mapper.AuditLogMapper;
import com.example.sep.service.EventRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 审计监听器单测。
 */
class AuditLogListenerTest {

    private AuditLogMapper auditLogMapper;
    private EventRecordService eventRecordService;
    private AuditLogListener listener;

    @BeforeEach
    void setUp() {
        auditLogMapper = mock(AuditLogMapper.class);
        eventRecordService = mock(EventRecordService.class);
        listener = new AuditLogListener(auditLogMapper, eventRecordService);
    }

    @Test
    @DisplayName("收到提交事件：写 LEAVE_SUBMIT 审计日志并回写消费明细")
    void handle() {
        LeaveSubmittedEvent event = new LeaveSubmittedEvent(
                7L, "zhangsan", "SICK", "感冒发烧", LocalDate.now(), LocalDate.now().plusDays(2));
        event.setEventId(55L);

        listener.handle(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("LEAVE_SUBMIT", saved.getAction());
        assertEquals("zhangsan", saved.getOperator());
        assertTrue(saved.getDetail().contains("单号 7"));

        verify(eventRecordService).markHandled(55L, "AuditLogListener");
    }
}

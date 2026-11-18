package com.example.sep.listener;

import com.example.sep.entity.Notify;
import com.example.sep.event.LeaveApprovedEvent;
import com.example.sep.mapper.NotifyMapper;
import com.example.sep.service.EventRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 通知监听器单测：审批通过与拒绝两种分支的通知文案。
 */
class NotifyListenerTest {

    private NotifyMapper notifyMapper;
    private EventRecordService eventRecordService;
    private NotifyListener listener;

    @BeforeEach
    void setUp() {
        notifyMapper = mock(NotifyMapper.class);
        eventRecordService = mock(EventRecordService.class);
        listener = new NotifyListener(notifyMapper, eventRecordService);
    }

    @Test
    @DisplayName("审批通过：通知标题含「审批通过」，未读，回写消费明细")
    void approved() {
        LeaveApprovedEvent event = new LeaveApprovedEvent(7L, "zhangsan", "ANNUAL", true, "admin");
        event.setEventId(66L);

        listener.handle(event);

        ArgumentCaptor<Notify> captor = ArgumentCaptor.forClass(Notify.class);
        verify(notifyMapper).insert(captor.capture());
        Notify saved = captor.getValue();
        assertEquals("zhangsan", saved.getReceiver());
        assertTrue(saved.getTitle().contains("审批通过"));
        assertEquals(0, saved.getReadFlag());
        verify(eventRecordService).markHandled(66L, "NotifyListener");
    }

    @Test
    @DisplayName("审批拒绝：通知标题含「被拒绝」")
    void rejected() {
        LeaveApprovedEvent event = new LeaveApprovedEvent(8L, "lisi", "CASUAL", false, "admin");

        listener.handle(event);

        ArgumentCaptor<Notify> captor = ArgumentCaptor.forClass(Notify.class);
        verify(notifyMapper).insert(captor.capture());
        assertTrue(captor.getValue().getTitle().contains("被拒绝"));
    }
}

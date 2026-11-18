package com.example.sep.listener;

import com.example.sep.event.LeaveApprovedEvent;
import com.example.sep.service.EventRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 统计监听器单测。
 */
class StatsListenerTest {

    private EventRecordService eventRecordService;
    private StatsListener listener;

    @BeforeEach
    void setUp() {
        eventRecordService = mock(EventRecordService.class);
        listener = new StatsListener(eventRecordService);
    }

    @Test
    @DisplayName("通过事件：计数 +1 并回写消费明细；拒绝事件不计数")
    void countOnlyApproved() {
        LeaveApprovedEvent approved = new LeaveApprovedEvent(1L, "a", "SICK", true, "admin");
        listener.handle(approved);
        assertEquals(1, listener.getApprovedCount());

        LeaveApprovedEvent rejected = new LeaveApprovedEvent(2L, "b", "SICK", false, "admin");
        listener.handle(rejected);
        assertEquals(1, listener.getApprovedCount(), "拒绝事件不能累加通过计数");

        verify(eventRecordService, times(2)).markHandled(any(), eq("StatsListener"));
    }
}

package com.example.sep.service;

import com.example.sep.entity.EventHandle;
import com.example.sep.entity.EventRecord;
import com.example.sep.mapper.EventHandleMapper;
import com.example.sep.mapper.EventRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 事件记录服务单测：本地消息表的落表与消费回写。
 */
class EventRecordServiceTest {

    private EventRecordMapper recordMapper;
    private EventHandleMapper handleMapper;
    private EventRecordService service;

    @BeforeEach
    void setUp() {
        recordMapper = mock(EventRecordMapper.class);
        handleMapper = mock(EventHandleMapper.class);
        service = new EventRecordService(recordMapper, handleMapper);
    }

    @Test
    @DisplayName("record：落表字段完整（类型/payload/状态0），返回自增 id")
    void record() {
        // mock insert 时回填自增主键（模拟 MyBatis-Plus IdType.AUTO 行为）
        when(recordMapper.insert(any(EventRecord.class))).thenAnswer(inv -> {
            EventRecord r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        });

        Long id = service.record("LeaveSubmittedEvent", "{\"leaveId\":1}");

        assertEquals(100L, id);
        ArgumentCaptor<EventRecord> captor = ArgumentCaptor.forClass(EventRecord.class);
        verify(recordMapper).insert(captor.capture());
        EventRecord saved = captor.getValue();
        assertEquals("LeaveSubmittedEvent", saved.getEventType());
        assertEquals(0, saved.getStatus(), "新事件状态必须是 0（待消费）");
        assertNotNull(saved.getCreateTime());
    }

    @Test
    @DisplayName("markHandled：插入消费明细 + 事件状态置 1")
    void markHandled() {
        service.markHandled(100L, "AuditLogListener");

        ArgumentCaptor<EventHandle> captor = ArgumentCaptor.forClass(EventHandle.class);
        verify(handleMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getEventId());
        assertEquals("AuditLogListener", captor.getValue().getListenerName());

        // 事件状态更新：update 条件带 id，状态置 1
        verify(recordMapper).update(any(EventRecord.class), argThat(w -> true));
    }
}

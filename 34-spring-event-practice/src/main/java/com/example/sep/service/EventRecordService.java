package com.example.sep.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sep.entity.EventHandle;
import com.example.sep.entity.EventRecord;
import com.example.sep.mapper.EventHandleMapper;
import com.example.sep.mapper.EventRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件记录服务：「本地消息表」思想的最小落地。
 *
 * <p>进程内事件的最大软肋：publishEvent 之后、监听器消费完成之前，
 * 应用宕机，事件就丢了。借鉴本地消息表模式——发布前先把事件落库
 * （与业务写库在同一个本地事务里，保证业务数据和事件记录同生共死），
 * 广播后再由监听器回写消费明细。生产可再进一步：定时任务扫
 * status=0 且超时未消费的事件重投，或用 MQ 事务消息替代。</p>
 *
 * <p>注意 record() 的默认 REQUIRED 传播不是笔误：本地消息表的核心
 * 就是「业务数据与事件记录同生共死」——record() 在业务事务内被调用，
 * 共享同一个事务，业务提交事件记录才提交，业务回滚事件记录一并回滚，
 * 永远不会出现「单据入库了事件却丢了」的中间态。而 markHandled()
 * 由异步监听器在业务事务之外调用，必须 REQUIRES_NEW 独立提交。</p>
 */
@Service
public class EventRecordService {

    private final EventRecordMapper eventRecordMapper;
    private final EventHandleMapper eventHandleMapper;

    public EventRecordService(EventRecordMapper eventRecordMapper,
                              EventHandleMapper eventHandleMapper) {
        this.eventRecordMapper = eventRecordMapper;
        this.eventHandleMapper = eventHandleMapper;
    }

    /**
     * 事件发布前落表，返回事件 id（挂在事件对象上供监听器回写）。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Long record(String eventType, String payloadJson) {
        EventRecord record = new EventRecord();
        record.setEventType(eventType);
        record.setPayload(payloadJson);
        record.setStatus(0);
        record.setCreateTime(LocalDateTime.now());
        eventRecordMapper.insert(record);
        return record.getId();
    }

    /**
     * 监听器消费完成后回写明细，并把事件状态置为已消费。
     * 单独事务：异步监听器与业务事务已无关，回写要独立提交。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markHandled(Long eventId, String listenerName) {
        EventHandle handle = new EventHandle();
        handle.setEventId(eventId);
        handle.setListenerName(listenerName);
        handle.setCreateTime(LocalDateTime.now());
        eventHandleMapper.insert(handle);

        EventRecord update = new EventRecord();
        update.setStatus(1);
        eventRecordMapper.update(update, new LambdaUpdateWrapper<EventRecord>()
                .eq(EventRecord::getId, eventId));
    }

    /** 查询某事件已消费的监听器名列表（事件中心页展示用） */
    public List<String> handledListeners(Long eventId) {
        return eventHandleMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EventHandle>()
                                .eq(EventHandle::getEventId, eventId)
                                .orderByAsc(EventHandle::getId))
                .stream()
                .map(EventHandle::getListenerName)
                .collect(Collectors.toList());
    }

    /** 分页查询事件流水 */
    public Page<EventRecord> page(long current, long size) {
        return eventRecordMapper.selectPage(
                new Page<>(current, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EventRecord>()
                        .orderByDesc(EventRecord::getId));
    }
}

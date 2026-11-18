package com.example.sep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sep.common.BusinessException;
import com.example.sep.dto.AuditLogVO;
import com.example.sep.dto.EventRecordVO;
import com.example.sep.dto.NotifyVO;
import com.example.sep.entity.AuditLog;
import com.example.sep.entity.EventHandle;
import com.example.sep.entity.EventRecord;
import com.example.sep.entity.Notify;
import com.example.sep.mapper.AuditLogMapper;
import com.example.sep.mapper.EventHandleMapper;
import com.example.sep.mapper.EventRecordMapper;
import com.example.sep.mapper.NotifyMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 事件中心 Service：事件流水、站内通知、审计日志的查询与 VO 装配。
 *
 * <p>EventRecordVO.handledBy 通过 event_handle 明细表反查，
 * 回答"这个事件被哪些监听器消费过"。</p>
 */
@Service
public class EventCenterService {

    private final EventRecordMapper eventRecordMapper;
    private final EventHandleMapper eventHandleMapper;
    private final NotifyMapper notifyMapper;
    private final AuditLogMapper auditLogMapper;

    public EventCenterService(EventRecordMapper eventRecordMapper,
                              EventHandleMapper eventHandleMapper,
                              NotifyMapper notifyMapper,
                              AuditLogMapper auditLogMapper) {
        this.eventRecordMapper = eventRecordMapper;
        this.eventHandleMapper = eventHandleMapper;
        this.notifyMapper = notifyMapper;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 事件流水分页：查 event_record，并按 event_id 装配 handledBy（消费监听器名列表）。
     */
    public Page<EventRecordVO> eventPage(long current, long size) {
        Page<EventRecord> page = eventRecordMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<EventRecord>().orderByDesc(EventRecord::getId));
        Map<Long, List<String>> handledMap = queryHandledMap(
                page.getRecords().stream().map(EventRecord::getId).collect(Collectors.toList()));

        Page<EventRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(r -> {
            EventRecordVO vo = new EventRecordVO();
            BeanUtils.copyProperties(r, vo);
            vo.setHandledBy(handledMap.getOrDefault(r.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 事件详情：单条事件记录 + 已消费监听器列表。
     */
    public EventRecordVO eventDetail(Long id) {
        EventRecord record = eventRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "事件记录不存在");
        }
        EventRecordVO vo = new EventRecordVO();
        BeanUtils.copyProperties(record, vo);
        vo.setHandledBy(queryHandledMap(Collections.singletonList(id))
                .getOrDefault(id, Collections.emptyList()));
        return vo;
    }

    /**
     * 站内通知分页（可按接收人过滤）。
     */
    public Page<NotifyVO> notifyPage(long current, long size, String receiver) {
        Page<Notify> page = notifyMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Notify>()
                        .eq(receiver != null && !receiver.isEmpty(), Notify::getReceiver, receiver)
                        .orderByDesc(Notify::getId));
        Page<NotifyVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(n -> {
            NotifyVO vo = new NotifyVO();
            BeanUtils.copyProperties(n, vo);
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 审计日志分页。
     */
    public Page<AuditLogVO> auditPage(long current, long size) {
        Page<AuditLog> page = auditLogMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getId));
        Page<AuditLogVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(a -> {
            AuditLogVO vo = new AuditLogVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 批量查询事件 id -> 消费监听器名列表 的映射。
     */
    private Map<Long, List<String>> queryHandledMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return eventHandleMapper.selectList(new LambdaQueryWrapper<EventHandle>()
                        .in(EventHandle::getEventId, eventIds)
                        .orderByAsc(EventHandle::getId))
                .stream()
                .collect(Collectors.groupingBy(EventHandle::getEventId,
                        Collectors.mapping(EventHandle::getListenerName, Collectors.toList())));
    }
}

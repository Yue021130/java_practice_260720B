package com.example.sep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sep.common.BusinessException;
import com.example.sep.dto.LeaveApproveDTO;
import com.example.sep.dto.LeaveSaveDTO;
import com.example.sep.dto.LeaveVO;
import com.example.sep.entity.Leave;
import com.example.sep.event.LeaveApprovedEvent;
import com.example.sep.event.LeaveSubmittedEvent;
import com.example.sep.mapper.LeaveMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 请假 Service：提交、审批、分页查询。
 *
 * <p>本章核心：业务动作 + 事件发布的完整链路——</p>
 * <ol>
 *   <li>提交/审批先落库；</li>
 *   <li>事件先写 event_record（本地消息表思想），再 publishEvent 广播，
 *       保证事件可追溯，即使监听器消费失败也有据可查；</li>
 *   <li>同步监听器（@EventListener）在当前线程执行完方法才返回；
 *       @TransactionalEventListener(AFTER_COMMIT) 的监听器则只在事务提交后触发。</li>
 * </ol>
 */
@Slf4j
@Service
public class LeaveService {

    /** 请假类型 -> 中文名，供前端展示 */
    private static final Map<String, String> TYPE_TEXT;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("SICK", "病假");
        map.put("CASUAL", "事假");
        map.put("ANNUAL", "年假");
        TYPE_TEXT = Collections.unmodifiableMap(map);
    }

    private final LeaveMapper leaveMapper;
    private final EventRecordService eventRecordService;
    private final ApplicationEventPublisher publisher;

    public LeaveService(LeaveMapper leaveMapper,
                        EventRecordService eventRecordService,
                        ApplicationEventPublisher publisher) {
        this.leaveMapper = leaveMapper;
        this.eventRecordService = eventRecordService;
        this.publisher = publisher;
    }

    /**
     * 请假类型转中文，未知类型原样返回。
     */
    public static String typeText(String type) {
        return TYPE_TEXT.getOrDefault(type, type);
    }

    /**
     * 提交请假单：落库（status=0 待审批）后发布 LeaveSubmittedEvent。
     *
     * <p>事件先落 event_record（本地消息表思想），再广播，保证事件可追溯；
     * 同步监听器在本线程执行完才返回，前端能立刻查到通知/日志。</p>
     */
    public LeaveVO submit(LeaveSaveDTO dto) {
        Leave leave = new Leave();
        leave.setUsername(dto.getUsername());
        leave.setType(dto.getType());
        leave.setReason(dto.getReason());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setStatus(0);
        leaveMapper.insert(leave);

        // 事件先落 event_record（本地消息表思想），再广播，保证事件可追溯
        Long eventId = eventRecordService.record("LeaveSubmittedEvent",
                String.format("{\"leaveId\":%d,\"username\":\"%s\",\"type\":\"%s\"}",
                        leave.getId(), dto.getUsername(), dto.getType()));

        LeaveSubmittedEvent event = new LeaveSubmittedEvent(
                leave.getId(), dto.getUsername(), dto.getType(),
                dto.getReason(), dto.getStartDate(), dto.getEndDate());
        event.setEventId(eventId);
        publisher.publishEvent(event);

        log.info("用户 [{}] 提交请假单 [{}]，事件记录 [{}]", dto.getUsername(), leave.getId(), eventId);
        return toVO(leave);
    }

    /**
     * 审批通过。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO approve(Long id, String approver) {
        return doApprove(id, approver, true);
    }

    /**
     * 审批拒绝。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO reject(Long id, String approver) {
        return doApprove(id, approver, false);
    }

    /**
     * 审批公共逻辑：校验状态 -> 更新 -> 发布 LeaveApprovedEvent。
     *
     * <p>注意：@TransactionalEventListener(AFTER_COMMIT) 的监听器只会在
     * 本事务提交后触发，所以这里方法必须有事务。</p>
     */
    private LeaveVO doApprove(Long id, String approver, boolean approved) {
        Leave leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BusinessException(404, "请假单不存在");
        }
        if (leave.getStatus() != 0) {
            throw new BusinessException(400, "该请假单已审批，请勿重复操作");
        }

        leave.setStatus(approved ? 1 : 2);
        leave.setApprover(approver);
        leaveMapper.updateById(leave);

        // 同样先落事件记录再广播；AFTER_COMMIT 监听器依赖本方法的事务
        Long eventId = eventRecordService.record("LeaveApprovedEvent",
                String.format("{\"leaveId\":%d,\"username\":\"%s\",\"approved\":%s,\"approver\":\"%s\"}",
                        leave.getId(), leave.getUsername(), approved, approver));

        LeaveApprovedEvent event = new LeaveApprovedEvent(
                leave.getId(), leave.getUsername(), leave.getType(), approved, approver);
        event.setEventId(eventId);
        publisher.publishEvent(event);

        log.info("请假单 [{}] 被 [{}] {}", id, approver, approved ? "通过" : "拒绝");
        return toVO(leave);
    }

    /**
     * 分页查询请假单。
     */
    public Page<LeaveVO> page(long current, long size, String username, Integer status) {
        LambdaQueryWrapper<Leave> wrapper = new LambdaQueryWrapper<Leave>()
                .eq(username != null && !username.isEmpty(), Leave::getUsername, username)
                .eq(status != null, Leave::getStatus, status)
                .orderByDesc(Leave::getId);
        Page<Leave> page = leaveMapper.selectPage(new Page<>(current, size), wrapper);
        Page<LeaveVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 请假单详情。
     */
    public LeaveVO detail(Long id) {
        Leave leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BusinessException(404, "请假单不存在");
        }
        return toVO(leave);
    }

    private LeaveVO toVO(Leave leave) {
        LeaveVO vo = new LeaveVO();
        BeanUtils.copyProperties(leave, vo);
        return vo;
    }
}

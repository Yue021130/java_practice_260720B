package com.example.sep.listener;

import com.example.sep.entity.Notify;
import com.example.sep.event.LeaveApprovedEvent;
import com.example.sep.mapper.NotifyMapper;
import com.example.sep.service.EventRecordService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * 通知监听器：监听「审批结果」事件，发站内通知。
 *
 * <p>两个注解叠加是本章核心：</p>
 * <ul>
 *   <li>{@code @TransactionalEventListener(phase = AFTER_COMMIT)}：
 *   事件必须在事务提交后才投递给本监听器。LeaveService.approve 在
 *   事务里更新单据状态后发布事件——如果用普通 @EventListener，
 *   监听器在事务提交前执行，此时读库看到的还是「待审批」旧状态，
 *   用户收到「已通过」通知点进去却是待审批，这就是三篇参考文档里
 *   「被坑」的典型场景。AFTER_COMMIT 保证通知发出时数据已定稿。</li>
 *   <li>{@code @Async("eventExecutor")}：事务提交后异步执行，
 *   不拖慢审批接口的响应时间。注意 @Async 没有事务上下文，
 *   NotifyMapper 的写库靠 markHandled 的 REQUIRES_NEW 独立提交。</li>
 * </ul>
 *
 * <p>坑提醒：@TransactionalEventListener 在非事务方法发布事件时
 * 默认不执行（fallbackExecution=false），所以发布方方法必须有
 * @Transactional——LeaveService.approve 上已标注，删了就踩坑。</p>
 */
@Component
public class NotifyListener {

    private final NotifyMapper notifyMapper;
    private final EventRecordService eventRecordService;

    public NotifyListener(NotifyMapper notifyMapper,
                          EventRecordService eventRecordService) {
        this.notifyMapper = notifyMapper;
        this.eventRecordService = eventRecordService;
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LeaveApprovedEvent event) {
        Notify notify = new Notify();
        notify.setReceiver(event.getUsername());
        notify.setTitle(String.format("请假单 %d %s", event.getLeaveId(),
                event.isApproved() ? "审批通过" : "被拒绝"));
        notify.setContent(String.format("您的%s请假单（单号 %d）已被 %s %s。",
                event.getType(), event.getLeaveId(), event.getApprover(),
                event.isApproved() ? "审批通过" : "拒绝"));
        notify.setReadFlag(0);
        notify.setCreateTime(LocalDateTime.now());
        notifyMapper.insert(notify);

        eventRecordService.markHandled(event.getEventId(), "NotifyListener");
    }
}

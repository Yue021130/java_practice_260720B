package com.example.ae.listener;

import com.example.ae.entity.NotifyLog;
import com.example.ae.event.OrderPaidEvent;
import com.example.ae.repository.NotifyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 积分发放监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsListener {

    private final NotifyLogRepository notifyLogRepository;

    @Async("taskExecutor")
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        String threadName = Thread.currentThread().getName();
        log.info("[PointsListener] 订单 {} 发放积分，金额：{}，线程：{}",
                event.getOrderNo(), event.getAmount(), threadName);

        sleep(200);

        saveLog(event, threadName, "积分发放成功：" + event.getAmount().intValue());
    }

    private void saveLog(OrderPaidEvent event, String threadName, String result) {
        NotifyLog logEntity = new NotifyLog();
        logEntity.setOrderNo(event.getOrderNo());
        logEntity.setNotifyType("POINTS");
        logEntity.setThreadName(threadName);
        logEntity.setResult(result);
        logEntity.setCreateTime(LocalDateTime.now());
        notifyLogRepository.save(logEntity);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

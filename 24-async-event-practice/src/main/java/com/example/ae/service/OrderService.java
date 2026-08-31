package com.example.ae.service;

import com.example.ae.entity.NotifyLog;
import com.example.ae.entity.Order;
import com.example.ae.event.OrderPaidEvent;
import com.example.ae.repository.NotifyLogRepository;
import com.example.ae.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 订单服务：模拟下单、支付、事件发布。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotifyLogRepository notifyLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建订单。
     */
    @Transactional
    public Map<String, Object> createOrder(Long userId, BigDecimal amount) {
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNo", order.getOrderNo());
        map.put("status", "待支付");
        map.put("amount", amount);
        return map;
    }

    /**
     * 支付订单：更新状态并发布 OrderPaidEvent。
     *
     * <p>事件发布后，SmsListener / EmailListener / PointsListener 会异步处理。</p>
     */
    @Transactional
    public Map<String, Object> payOrder(String orderNo) {
        Order order = orderRepository.findAll().stream()
                .filter(o -> o.getOrderNo().equals(orderNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单不存在：" + orderNo));

        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);

        // 发布事件：解耦主流程与后续通知
        OrderPaidEvent event = new OrderPaidEvent(this, orderNo, order.getUserId(), order.getAmount());
        eventPublisher.publishEvent(event);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNo", orderNo);
        map.put("status", "已支付");
        map.put("tip", "短信/邮件/积分已异步下发");
        return map;
    }

    /**
     * 同步支付：不发布事件，直接串行调用，用于对比异步性能。
     */
    @Transactional
    public Map<String, Object> payOrderSync(String orderNo) {
        Order order = orderRepository.findAll().stream()
                .filter(o -> o.getOrderNo().equals(orderNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单不存在：" + orderNo));

        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);

        // 模拟同步调用
        sleep(300);
        sleep(500);
        sleep(200);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNo", orderNo);
        map.put("status", "已支付");
        map.put("tip", "同步完成：短信+邮件+积分串行处理");
        return map;
    }

    /**
     * 查询订单的通知日志。
     */
    public Map<String, Object> queryNotifyLogs(String orderNo) {
        List<NotifyLog> logs = notifyLogRepository.findByOrderNo(orderNo);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNo", orderNo);
        map.put("logCount", logs.size());
        map.put("logs", logs);
        return map;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

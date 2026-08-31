package com.example.ae.service;

import com.example.ae.repository.NotifyLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单服务测试：验证异步事件发布与监听。
 */
@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private NotifyLogRepository notifyLogRepository;

    @Test
    void payOrder_shouldPublishAsyncEvent() throws Exception {
        Map<String, Object> createResult = orderService.createOrder(1L, BigDecimal.valueOf(99.99));
        String orderNo = (String) createResult.get("orderNo");

        orderService.payOrder(orderNo);

        // 等待异步监听器完成
        TimeUnit.SECONDS.sleep(2);

        assertThat(notifyLogRepository.findByOrderNo(orderNo))
                .hasSize(3)
                .anyMatch(log -> "SMS".equals(log.getNotifyType()))
                .anyMatch(log -> "EMAIL".equals(log.getNotifyType()))
                .anyMatch(log -> "POINTS".equals(log.getNotifyType()));
    }

    @Test
    void payOrderSync_shouldNotCreateNotifyLogs() {
        Map<String, Object> createResult = orderService.createOrder(2L, BigDecimal.valueOf(88.88));
        String orderNo = (String) createResult.get("orderNo");

        orderService.payOrderSync(orderNo);

        assertThat(notifyLogRepository.findByOrderNo(orderNo)).isEmpty();
    }
}

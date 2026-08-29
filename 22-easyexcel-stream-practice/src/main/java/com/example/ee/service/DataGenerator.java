package com.example.ee.service;

import com.example.ee.entity.Order;
import com.example.ee.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟数据生成器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataGenerator {

    private static final int BATCH_SIZE = 5000;

    private final OrderRepository orderRepository;

    /**
     * 生成指定数量的订单数据。
     */
    public Map<String, Object> generate(long count) {
        if (count <= 0 || count > 2_000_000) {
            throw new IllegalArgumentException("生成数量需在 1 ~ 2000000 之间");
        }

        orderRepository.deleteAll();

        String[] products = {"iPhone 15", "MacBook Pro", "AirPods", "iPad", "机械键盘", "显示器", "鼠标", "充电宝"};
        String[] statuses = {"待付款", "已付款", "已发货", "已完成", "已取消"};
        LocalDateTime now = LocalDateTime.now();

        List<Order> buffer = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            Order order = new Order();
            order.setOrderNo("ORD" + String.format("%012d", i));
            order.setUserId(10000L + i);
            order.setUsername("user" + (i % 10000));
            order.setProductName(products[(int) (i % products.length)]);
            order.setAmount(BigDecimal.valueOf((i % 1000) + 1 + 0.99));
            order.setStatus(statuses[(int) (i % statuses.length)]);
            order.setOrderTime(now.minusDays(i % 365));
            order.setAddress("湖北省武汉市洪山区街道" + (i % 1000) + "号");
            order.setRemark("备注" + i);
            buffer.add(order);

            if (buffer.size() >= BATCH_SIZE) {
                orderRepository.saveAll(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            orderRepository.saveAll(buffer);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("generated", count);
        map.put("actualCount", orderRepository.count());
        map.put("tip", "数据已写入 H2 内存数据库，可直接测试导出");
        return map;
    }
}

package com.example.ae.web;

import com.example.ae.common.ApiResponse;
import com.example.ae.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单 REST 接口。
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单与异步事件", description = "下单、支付、事件监听、八股速记")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public ApiResponse<Map<String, Object>> create(@RequestParam Long userId,
                                                   @RequestParam BigDecimal amount) {
        return ApiResponse.ok(orderService.createOrder(userId, amount));
    }

    @PostMapping("/pay")
    @Operation(summary = "支付订单（异步事件）")
    public ApiResponse<Map<String, Object>> pay(@RequestParam String orderNo) {
        return ApiResponse.ok(orderService.payOrder(orderNo));
    }

    @PostMapping("/pay-sync")
    @Operation(summary = "支付订单（同步处理，用于对比）")
    public ApiResponse<Map<String, Object>> paySync(@RequestParam String orderNo) {
        return ApiResponse.ok(orderService.payOrderSync(orderNo));
    }

    @GetMapping("/notify-logs")
    @Operation(summary = "查询订单通知日志")
    public ApiResponse<Map<String, Object>> notifyLogs(@RequestParam String orderNo) {
        return ApiResponse.ok(orderService.queryNotifyLogs(orderNo));
    }

    @GetMapping("/explain")
    @Operation(summary = "八股速记：@Async + Spring Event")
    public ApiResponse<Map<String, Object>> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "@Async + Spring Event 核心八股");

        Map<String, String> points = new LinkedHashMap<>();
        points.put("@EnableAsync", "必须在配置类或启动类开启，@Async 才会生效。");
        points.put("默认线程池", "SimpleAsyncExecutor，每次创建新线程，生产环境必须自定义。");
        points.put("自定义线程池", "通过 ThreadPoolTaskExecutor 注入，@Async(\"beanName\") 使用。");
        points.put("@EventListener", "Spring 事件监听，支持同步/异步。");
        points.put("ApplicationEventPublisher", "用于发布事件，解耦业务主流程。");
        points.put("事务与事件", "事件发布在事务内，监听器默认不感知事务状态；可用 @TransactionalEventListener。");
        points.put("异常处理", "异步方法异常不会抛给调用方，需配置 AsyncUncaughtExceptionHandler。");
        result.put("points", points);

        return ApiResponse.ok(result);
    }
}

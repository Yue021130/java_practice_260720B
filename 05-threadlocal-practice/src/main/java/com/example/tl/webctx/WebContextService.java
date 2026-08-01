package com.example.tl.webctx;

import com.example.tl.context.DateFormatHolder;
import com.example.tl.context.TraceContext;
import com.example.tl.context.UserContext;
import com.example.tl.context.dto.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Web 请求上下文场景演示。
 */
@Service
public class WebContextService {

    private static final Logger log = LoggerFactory.getLogger(WebContextService.class);

    /**
     * 演示 Filter + ThreadLocal 在 Controller/Service 中无感传递用户信息。
     */
    public Map<String, Object> userContextDemo() {
        Map<String, Object> result = new HashMap<>();
        User user = UserContext.get();
        result.put("userFromThreadLocal", user);
        result.put("currentThread", Thread.currentThread().getName());
        result.put("note", "Filter 在请求进入时 set，Controller/Service 直接 UserContext.get() 取值，请求结束 finally remove");
        return result;
    }

    /**
     * 演示 MDC + traceId 全链路日志。
     */
    public Map<String, Object> mdcTraceDemo() {
        Map<String, Object> result = new HashMap<>();
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        List<String> logs = new ArrayList<>();

        MDC.put("traceId", traceId);
        TraceContext.set(traceId);
        try {
            logAndRecord(logs, traceId, "收到订单创建请求");
            logAndRecord(logs, traceId, "校验库存通过");
            logAndRecord(logs, traceId, "扣减余额成功");
        } finally {
            MDC.clear();
            TraceContext.clear();
        }

        result.put("traceId", traceId);
        result.put("logs", logs);
        result.put("note", "MDC 底层也是 ThreadLocal；日志 pattern 中 %X{traceId} 会在输出时替换为当前线程的 traceId");
        return result;
    }

    private void logAndRecord(List<String> logs, String traceId, String message) {
        log.info(message);
        logs.add(String.format("%s [%s] INFO  [%s] - %s",
                new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()),
                Thread.currentThread().getName(), traceId, message));
    }

    /**
     * 演示 SimpleDateFormat 并发问题与 ThreadLocal 解决。
     */
    public Map<String, Object> dateFormatSafeDemo() throws InterruptedException {
        Map<String, Object> result = new HashMap<>();
        String dateStr = "2024-01-15 10:30:00";
        int threadCount = 20;

        SimpleDateFormat shared = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int[] sharedStats = parseConcurrently(shared, dateStr, threadCount);

        int[] threadLocalStats = parseConcurrently(new SimpleDateFormat() {
            @Override
            public java.util.Date parse(String source) throws ParseException {
                return DateFormatHolder.get().parse(source);
            }
        }, dateStr, threadCount);

        result.put("sharedErrors", sharedStats[0]);
        result.put("sharedSuccess", sharedStats[1]);
        result.put("threadLocalErrors", threadLocalStats[0]);
        result.put("threadLocalSuccess", threadLocalStats[1]);
        result.put("note", "共享 SimpleDateFormat 并发 parse 会抛异常或结果错乱；每个线程一份 SimpleDateFormat 可彻底解决");
        return result;
    }

    private int[] parseConcurrently(SimpleDateFormat format, String dateStr, int threadCount) throws InterruptedException {
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    format.parse(dateStr);
                    success.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        return new int[]{errors.get(), success.get()};
    }
}

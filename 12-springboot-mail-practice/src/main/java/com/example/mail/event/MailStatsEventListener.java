package com.example.mail.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监听器二：统计监听器。
 *
 * 演示 @EventListener 的 condition 条件过滤 + 通过事件在内存中聚合统计。
 * 生产场景通常会落到 Redis / 时序库做监控告警。
 */
@Slf4j
@Component
public class MailStatsEventListener {

    /** 总发送数（成功） */
    private final AtomicLong totalSent = new AtomicLong();
    /** 总失败数 */
    private final AtomicLong totalFailed = new AtomicLong();
    /** 按场景标签统计的成功数 */
    private final Map<String, AtomicLong> sentByTag = new ConcurrentHashMap<>();
    /** 按场景标签统计的失败数 */
    private final Map<String, AtomicLong> failedByTag = new ConcurrentHashMap<>();

    /**
     * condition 里可用 SpEL 访问事件对象（#event）：
     * 这里把成功与失败拆成两个监听方法，比在方法内 if 更语义化。
     */
    @EventListener(condition = "#event.success")
    public void onSuccess(MailSentEvent event) {
        totalSent.incrementAndGet();
        String tag = event.getRecord() != null ? event.getRecord().getTag() : "unknown";
        sentByTag.computeIfAbsent(tag, k -> new AtomicLong()).incrementAndGet();
    }

    @EventListener(condition = "#event.success == false")
    public void onFailure(MailSentEvent event) {
        totalFailed.incrementAndGet();
        failedByTag.computeIfAbsent("unknown", k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * 导出统计快照。
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSent", totalSent.get());
        result.put("totalFailed", totalFailed.get());
        result.put("sentByTag", toCountMap(sentByTag));
        result.put("failedByTag", toCountMap(failedByTag));
        return result;
    }

    private Map<String, Long> toCountMap(Map<String, AtomicLong> source) {
        Map<String, Long> copy = new LinkedHashMap<>();
        source.forEach((k, v) -> copy.put(k, v.get()));
        return copy;
    }
}

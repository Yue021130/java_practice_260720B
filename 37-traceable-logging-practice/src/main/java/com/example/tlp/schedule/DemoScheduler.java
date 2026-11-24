package com.example.tlp.schedule;

import com.example.tlp.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：没有上游请求，必须自己开新链路。
 *
 * <p>不包 runWithNewTrace 的后果：定时任务跑在 Spring 的调度线程上，
 * MDC 里没有 traceId（[%X{traceId:-}] 输出空串），如果该线程此前
 * 被别的代码 set 过值还会串号。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "demo.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DemoScheduler {

    @Scheduled(fixedRate = 30000)
    public void run() {
        TraceContext.runWithNewTrace(() ->
                log.info("定时任务执行：每 30 秒一条新 traceId 链路"));
    }
}

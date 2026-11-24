package com.example.tlp.controller;

import com.example.tlp.event.DemoEventPublisher;
import com.example.tlp.service.DemoService;
import com.example.tlp.task.BatchTaskService;
import com.example.tlp.task.TaskProgress;
import com.example.tlp.trace.TraceContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 场景演示入口：每个接口验证一种 traceId 链路行为。
 * 对照方式：响应里的 traceId → 拿它 grep logs/ 目录下的文件。
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class LogDemoController {

    private final ThreadPoolTaskExecutor batchExecutor;
    private final DemoEventPublisher eventPublisher;
    private final BatchTaskService batchTaskService;
    private final DemoService demoService;

    public LogDemoController(ThreadPoolTaskExecutor batchExecutor,
                             DemoEventPublisher eventPublisher,
                             BatchTaskService batchTaskService,
                             DemoService demoService) {
        this.batchExecutor = batchExecutor;
        this.eventPublisher = eventPublisher;
        this.batchTaskService = batchTaskService;
        this.demoService = demoService;
    }

    /** 场景 1：同步链路 Controller → Service 风格 */
    @GetMapping("/demo/sync")
    public Map<String, String> sync() {
        log.info("sync start：模拟同步业务处理");
        work();
        log.info("sync end");
        return result();
    }

    /** 场景 2：@Async 异步线程 */
    @GetMapping("/demo/async")
    public Map<String, String> async() {
        log.info("async start：提交异步任务");
        demoService.asyncWork();
        log.info("async end（注意异步日志可能晚于本行打印）");
        return result();
    }

    /** 场景 3：CompletableFuture + 自定义线程池 */
    @GetMapping("/demo/future")
    public Map<String, String> future() throws Exception {
        log.info("future start");
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> log.info("CompletableFuture 异步任务"), batchExecutor);
        future.get();
        log.info("future end");
        return result();
    }

    /** 场景 4：模拟 MQ 事件发布 → @Async 消费 */
    @GetMapping("/demo/event")
    public Map<String, String> event() {
        log.info("发布事件（模拟发消息到 MQ）");
        eventPublisher.publish("ORDER-" + System.currentTimeMillis());
        return result();
    }

    /** 场景 5：异常链路（全局异常处理器打 ERROR，同 traceId） */
    @GetMapping("/demo/exception")
    public Map<String, String> exception() {
        log.info("exception start：即将抛异常");
        throw new IllegalStateException("模拟业务异常");
    }

    /** 场景 6：前端透传——返回收到的 traceId，应与请求头一致 */
    @GetMapping("/demo/headers")
    public Map<String, String> headers() {
        return result();
    }

    private void work() {
        log.info("doWork：同步链路中间层");
    }

    private Map<String, String> result() {
        Map<String, String> map = new HashMap<>();
        map.put("traceId", TraceContext.get());
        return map;
    }

    // ==================== 批次任务（前端进度可视化） ====================

    @Data
    public static class BatchSubmitRequest {
        /** 子任务数 */
        private int taskCount = 20;
        /** 单任务耗时毫秒 */
        private long perTaskMs = 300;
        /** 千分比失败率，演示失败计数 */
        private int failRatePermille = 0;
    }

    @PostMapping("/batch/submit")
    public Map<String, String> submitBatch(@RequestBody BatchSubmitRequest request) {
        String taskId = batchTaskService.submit(
                Math.max(1, Math.min(request.getTaskCount(), 500)),
                Math.max(50, Math.min(request.getPerTaskMs(), 5000)),
                Math.max(0, Math.min(request.getFailRatePermille(), 200)));
        Map<String, String> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("traceId", MDC.get(TraceContext.TRACE_ID));
        return map;
    }

    @GetMapping("/batch/{taskId}/progress")
    public TaskProgress progress(@PathVariable String taskId) {
        return batchTaskService.getProgress(taskId);
    }
}

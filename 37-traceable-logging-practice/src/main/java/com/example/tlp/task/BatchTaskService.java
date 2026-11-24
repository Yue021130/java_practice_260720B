package com.example.tlp.task;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批次任务执行器：把一个大任务拆成 N 个子任务丢进线程池，
 * 并实时维护进度供前端展示。
 *
 * <p>traceId 语义说明：整个批次共享「提交时刻」的 traceId——
 * 批次内所有子任务的日志用同一个 id 串联，与触发它的 HTTP 请求
 * 也同链路；这是「业务批次」和「单个请求」两种粒度的体现。</p>
 */
@Slf4j
@Service
public class BatchTaskService {

    /** 进度内存表：taskId → 进度（单例 Service 持有，全应用共享） */
    private final Map<String, TaskProgress> progressMap = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor batchExecutor;

    public BatchTaskService(ThreadPoolTaskExecutor batchExecutor) {
        this.batchExecutor = batchExecutor;
    }

    /**
     * 提交批次任务。
     *
     * @param taskCount    子任务数
     * @param perTaskMs    单任务模拟耗时（毫秒）
     * @param failRatePermille 千分比失败率（演示失败计数，0~100）
     * @return taskId，前端凭它轮询进度
     */
    public String submit(int taskCount, long perTaskMs, int failRatePermille) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        // 提交时刻的快照：整个批次共用一个 traceId（MDC 由 TaskDecorator 透传进线程池）
        String batchTraceId = MDC.get("traceId");

        TaskProgress progress = new TaskProgress();
        progress.setTaskId(taskId);
        progress.setTraceId(batchTraceId);
        progress.setTotal(taskCount);
        progress.setStartTime(LocalDateTime.now());
        progressMap.put(taskId, progress);

        log.info("批次 [{}] 提交：{} 个子任务，单任务 {}ms", taskId, taskCount, perTaskMs);

        AtomicInteger seq = new AtomicInteger(0);
        for (int i = 0; i < taskCount; i++) {
            int index = seq.incrementAndGet();
            batchExecutor.submit(() -> runSubTask(progress, index, perTaskMs, failRatePermille));
        }
        return taskId;
    }

    private void runSubTask(TaskProgress progress, int index, long perTaskMs, int failRatePermille) {
        // 线程池线程已由 MdcTaskDecorator 恢复 MDC，这里直接能拿到批次 traceId
        log.info("批次 [{}] 子任务 {}/{} 开始", progress.getTaskId(), index, progress.getTotal());
        try {
            TimeUnit.MILLISECONDS.sleep(perTaskMs);
            // 伪随机失败：用 index 做确定性，避免测试/演示抖动不可复现
            if (failRatePermille > 0 && index * 37 % 1000 < failRatePermille) {
                throw new IllegalStateException("子任务 " + index + " 模拟失败");
            }
            progress.getDone().incrementAndGet();
            log.info("批次 [{}] 子任务 {}/{} 完成", progress.getTaskId(), index, progress.getTotal());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            progress.getFailed().incrementAndGet();
        } catch (Exception e) {
            progress.getFailed().incrementAndGet();
            log.warn("批次 [{}] 子任务 {}/{} 失败：{}", progress.getTaskId(), index,
                    progress.getTotal(), e.getMessage());
        }
    }

    /** 前端轮询进度 */
    public TaskProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }
}

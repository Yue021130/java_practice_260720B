package com.example.threadpool.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟任务提交服务。
 *
 * 任务内容很简单：sleep taskDurationMs 毫秒，模拟一个耗时操作
 * （可以理解为一次 IO 调用或一段计算）。重点不在任务本身，
 * 而在于观察大量任务涌入时线程池各指标的变化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSubmitService {

    /** Spring 会把所有 ThreadPoolTaskExecutor 类型的 Bean 按 Bean 名注入到这个 Map */
    private final Map<String, ThreadPoolTaskExecutor> poolMap;

    /** 全局任务序号，用于日志区分任务 */
    private final AtomicLong taskSeq = new AtomicLong(0);

    /**
     * 向指定线程池提交 count 个模拟任务。
     *
     * @param poolName       池名（cpuPool / ioPool / customPool）
     * @param count          提交任务数
     * @param taskDurationMs 每个任务的模拟耗时（毫秒）
     * @return 实际成功进入线程池的任务数（被 AbortPolicy 抛异常拒绝的不计入）
     */
    public int submitTasks(String poolName, int count, long taskDurationMs) {
        ThreadPoolTaskExecutor executor = getPool(poolName);
        int submitted = 0;
        for (int i = 0; i < count; i++) {
            long seq = taskSeq.incrementAndGet();
            try {
                executor.execute(() -> runMockTask(poolName, seq, taskDurationMs));
                submitted++;
            } catch (RejectedExecutionException e) {
                // 默认 AbortPolicy 会抛异常（cpuPool / ioPool 打满时）。
                // 这里捕获并中断提交，把“成功提交了多少个”如实返回给调用方。
                log.warn("任务 seq={} 被线程池 [{}] 拒绝（AbortPolicy 抛异常），已成功提交 {} 个",
                        seq, poolName, submitted);
                break;
            }
        }
        log.info("向线程池 [{}] 提交任务完成：请求 {} 个，成功入池 {} 个，单任务耗时 {}ms",
                poolName, count, submitted, taskDurationMs);
        return submitted;
    }

    /** 模拟任务体：睡一会再醒来，打上线程名日志方便观察是哪个池的哪个线程在执行 */
    private void runMockTask(String poolName, long seq, long taskDurationMs) {
        String threadName = Thread.currentThread().getName();
        log.debug("任务 seq={} 开始执行，pool={}，thread={}", seq, poolName, threadName);
        try {
            Thread.sleep(taskDurationMs);
        } catch (InterruptedException e) {
            // 恢复中断标记，是处理 InterruptedException 的规范姿势
            Thread.currentThread().interrupt();
        }
        log.debug("任务 seq={} 执行完毕，pool={}，thread={}", seq, poolName, threadName);
    }

    /** 按池名取池，不存在时抛出带中文提示的异常，由 Controller 转成 400 响应 */
    public ThreadPoolTaskExecutor getPool(String poolName) {
        ThreadPoolTaskExecutor executor = poolMap.get(poolName);
        if (executor == null) {
            throw new IllegalArgumentException(
                    "线程池不存在：" + poolName + "，可选值：" + poolMap.keySet());
        }
        return executor;
    }
}

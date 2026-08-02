package com.example.async.task;

import com.example.async.support.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 核心业务异步方法集合：演示 fire-forget、Future、CompletableFuture、自定义线程池、异常等场景。
 */
@Slf4j
@Service
public class AsyncTaskService {

    @Resource(name = "ioPool")
    private ThreadPoolTaskExecutor ioPool;

    private final ConcurrentHashMap<String, String> taskStatus = new ConcurrentHashMap<>();

    /**
     * 无返回值 fire-and-forget：把任务状态写入内存 Map。
     */
    @Async
    public void fireAndForget(String taskId) {
        taskStatus.put(taskId, "RUNNING:" + Thread.currentThread().getName());
        sleepQuietly(50);
        taskStatus.put(taskId, "DONE:" + Thread.currentThread().getName());
    }

    public String getStatus(String taskId) {
        return taskStatus.getOrDefault(taskId, "NOT_FOUND");
    }

    /**
     * CompletableFuture 返回值：模拟 CPU 小任务（斐波那契数列）。
     */
    @Async
    public CompletableFuture<Long> computeAsync(int n) {
        return CompletableFuture.completedFuture(fib(n));
    }

    /**
     * Future 返回值 + 模拟耗时：供调用方演示带超时的 get(timeout, TimeUnit)。
     */
    @Async
    public Future<Integer> submitWithTimeout(int n) {
        sleepQuietly(300);
        return CompletableFuture.completedFuture(n * n);
    }

    /**
     * 指定 cpuPool 执行，返回线程名用于验证是否走了指定池。
     */
    @Async("cpuPool")
    public CompletableFuture<String> runOnCpuPool() {
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }

    /**
     * 指定 ioPool 执行。
     */
    @Async("ioPool")
    public CompletableFuture<String> runOnIoPool() {
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }

    /**
     * void 异步方法抛异常：由 AsyncUncaughtExceptionHandler 捕获。
     */
    @Async
    public void throwException() {
        throw new RuntimeException("void 异步方法异常");
    }

    /**
     * CompletableFuture 异步方法抛异常：调用方可用 exceptionally / handle 处理。
     */
    @Async
    public CompletableFuture<String> throwInFuture() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("CompletableFuture 异步异常"));
        return future;
    }

    /**
     * 上下文透传验证：在异步线程读取 ThreadLocal traceId。
     */
    @Async
    public CompletableFuture<String> readTraceIdAsync() {
        return CompletableFuture.completedFuture(TraceContext.get());
    }

    /**
     * 模拟一个可被聚合的异步 IO 任务。
     */
    @Async("ioPool")
    public CompletableFuture<Integer> mockIoTask(int index) {
        sleepQuietly(150);
        return CompletableFuture.completedFuture(index * 10);
    }

    /**
     * 同步版本：串行调用 3 个 sleep 任务，用于与异步做耗时对比。
     */
    public long syncThreeTasks() {
        long start = System.currentTimeMillis();
        sleepQuietly(200);
        sleepQuietly(200);
        sleepQuietly(200);
        return System.currentTimeMillis() - start;
    }

    /**
     * 异步版本：并行调用 3 个 sleep 任务。
     */
    public long asyncThreeTasks() {
        long start = System.currentTimeMillis();
        CompletableFuture<Void> all = CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> sleepQuietly(200), ioPool),
                CompletableFuture.runAsync(() -> sleepQuietly(200), ioPool),
                CompletableFuture.runAsync(() -> sleepQuietly(200), ioPool)
        );
        all.join();
        return System.currentTimeMillis() - start;
    }

    private long fib(int n) {
        if (n <= 1) return n;
        long a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            long t = a + b;
            a = b;
            b = t;
        }
        return b;
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}

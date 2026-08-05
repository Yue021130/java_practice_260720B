package com.example.exception.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * 并发中的异常场景服务。
 */
@Slf4j
@Service
public class ConcurrencyScenarioService {

    /**
     * 演示子线程异常不会抛给主线程。
     */
    public Map<String, Object> threadUncaught() throws InterruptedException {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> logs = new ArrayList<>();

        Thread t = new Thread(() -> {
            throw new RuntimeException("子线程异常");
        });
        t.start();
        t.join();

        logs.add("子线程异常默认只会打印到控制台，不会抛给主线程");
        logs.add("如需统一处理，设置 Thread.setDefaultUncaughtExceptionHandler");
        result.put("logs", logs);
        return result;
    }

    /**
     * 演示 UncaughtExceptionHandler。
     */
    public Map<String, Object> uncaughtHandler() throws InterruptedException {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> logs = Collections.synchronizedList(new ArrayList<>());

        Thread t = new Thread(() -> {
            throw new RuntimeException("子线程异常被 handler 捕获");
        });
        t.setUncaughtExceptionHandler((thread, ex) -> {
            logs.add("Handler 捕获: " + thread.getName() + " -> " + ex.getMessage());
        });
        t.start();
        t.join();

        result.put("logs", logs);
        result.put("tip", "生产上可在此 handler 中记录日志、上报监控、发送告警");
        return result;
    }

    /**
     * 演示 Future.get 对异常的包装。
     */
    public Map<String, Object> futureGet() {
        Map<String, Object> result = new LinkedHashMap<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<Integer> future = pool.submit(() -> 1 / 0);
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("interrupted", true);
        } catch (ExecutionException e) {
            result.put("caughtType", e.getClass().getName());
            result.put("causeType", e.getCause().getClass().getName());
            result.put("causeMessage", e.getCause().getMessage());
            result.put("tip", "Future.get 把任务异常包装成 ExecutionException，实际异常在 cause 中");
        } finally {
            pool.shutdown();
        }
        return result;
    }

    /**
     * 演示 CompletableFuture 的异常处理 API。
     */
    public Map<String, Object> completableException() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> logs = new ArrayList<>();

        // exceptionally：捕获异常并返回默认值，不传播异常
        Integer r1 = CompletableFuture.supplyAsync(() -> 1 / 0)
                .exceptionally(ex -> {
                    logs.add("exceptionally 捕获: " + ex.getMessage());
                    return -1;
                })
                .join();
        logs.add("exceptionally 结果: " + r1);

        // handle：无论成功失败都执行，可访问异常和结果
        Integer r2 = CompletableFuture.supplyAsync(() -> 1 / 0)
                .handle((res, ex) -> {
                    if (ex != null) {
                        logs.add("handle 捕获异常: " + ex.getMessage());
                        return -2;
                    }
                    return res;
                })
                .join();
        logs.add("handle 结果: " + r2);

        // whenComplete：只消费不修改结果，异常继续传播
        try {
            CompletableFuture.supplyAsync(() -> 1 / 0)
                    .whenComplete((res, ex) -> logs.add("whenComplete 看到异常: " + (ex != null)))
                    .join();
        } catch (CompletionException e) {
            logs.add("whenComplete 后异常继续传播: " + e.getCause().getClass().getSimpleName());
        }

        result.put("logs", logs);
        result.put("summary", "exceptionally 返回默认值；handle 统一处理两种结果；whenComplete 不吞异常");
        return result;
    }

    /**
     * 演示 @Async 异常由 AsyncUncaughtExceptionHandler 捕获。
     */
    public Map<String, Object> asyncException() {
        Map<String, Object> result = new LinkedHashMap<>();
        // 触发异步异常，由 AsyncConfig 中的 handler 处理
        // 为了等待日志输出，这里同步返回说明即可
        result.put("message", "已触发 @Async 异常，请查看后端控制台日志（CustomAsyncExceptionHandler 会打印）");
        return result;
    }

    /**
     * 演示线程池 submit 吞异常 vs execute 打印。
     */
    public Map<String, Object> poolSwallow() throws InterruptedException {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> logs = Collections.synchronizedList(new ArrayList<>());

        ExecutorService pool = Executors.newFixedThreadPool(1);

        // submit 吞异常：异常被 Future 对象持有，不调用 get() 就看不到
        pool.submit(() -> {
            throw new RuntimeException("submit 任务异常（被吞）");
        });

        // execute 会打印异常到控制台
        pool.execute(() -> {
            throw new RuntimeException("execute 任务异常（打印）");
        });

        Thread.sleep(500);
        logs.add("submit 的异常被 Future 包装，不 get 不抛");
        logs.add("execute 的异常会由线程组默认 handler 打印到控制台");

        pool.shutdown();
        result.put("logs", logs);
        result.put("tip", "生产上 submit 后务必处理 Future.get()，否则异常静默丢失");
        return result;
    }
}

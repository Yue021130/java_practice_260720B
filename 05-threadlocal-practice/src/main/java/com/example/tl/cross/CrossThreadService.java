package com.example.tl.cross;

import com.example.tl.context.InheritableContext;
import com.example.tl.context.TtlContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 跨线程 ThreadLocal 演示。
 */
@Service
public class CrossThreadService {

    /**
     * 专门用于演示线程池污染的 ThreadLocal。
     */
    private static final ThreadLocal<String> DEMO_LOCAL = new ThreadLocal<>();

    private final ExecutorService demoExecutor;
    private final ExecutorService ttlExecutor;

    public CrossThreadService(@Qualifier("demoExecutor") ExecutorService demoExecutor,
                              @Qualifier("ttlExecutor") ExecutorService ttlExecutor) {
        this.demoExecutor = demoExecutor;
        this.ttlExecutor = ttlExecutor;
    }

    /**
     * InheritableThreadLocal：子线程（new Thread）可继承父线程值；线程池不继承。
     */
    public Map<String, Object> inheritableDemo() throws InterruptedException, ExecutionException {
        Map<String, Object> result = new HashMap<>();
        InheritableContext.set("from-parent");

        // 1) 子线程继承
        Future<String> childFuture = CompletableFuture.supplyAsync(() -> {
            String value = InheritableContext.get();
            InheritableContext.clear();
            return value;
        }, runnable -> new Thread(runnable).start()); // 强制 new Thread

        // 2) 线程池不继承
        Future<String> poolFuture = demoExecutor.submit(() -> {
            String value = InheritableContext.get();
            return value == null ? "null" : value;
        });

        result.put("childThreadValue", childFuture.get());
        result.put("poolThreadValue", poolFuture.get());
        result.put("note", "InheritableThreadLocal 只能透传给 new Thread() 创建的子线程，对线程池无效");
        InheritableContext.clear();
        return result;
    }

    /**
     * 线程池污染：任务 A 设置 ThreadLocal 后未 remove，任务 B 复用同一线程时读到 A 的值。
     */
    public Map<String, Object> poolHazardDemo() throws InterruptedException, ExecutionException {
        Map<String, Object> result = new HashMap<>();

        demoExecutor.submit(() -> {
            DEMO_LOCAL.set("task-A-data");
            // 故意不 remove
        }).get();

        String leaked = demoExecutor.submit(() -> {
            String value = DEMO_LOCAL.get();
            DEMO_LOCAL.remove(); // 清理，避免影响后续测试
            return value == null ? "null" : value;
        }).get();

        result.put("leakedValue", leaked);
        result.put("note", "线程池复用线程，如果任务 A 没有 remove，任务 B 可能读到 A 的残留数据（串号）");
        return result;
    }

    /**
     * 线程池正确使用：任务 finally 中 remove。
     */
    public Map<String, Object> poolRemoveDemo() throws InterruptedException, ExecutionException {
        Map<String, Object> result = new HashMap<>();

        demoExecutor.submit(() -> {
            DEMO_LOCAL.set("task-C-data");
            try {
                return DEMO_LOCAL.get();
            } finally {
                DEMO_LOCAL.remove();
            }
        }).get();

        String nextValue = demoExecutor.submit(() -> {
            String value = DEMO_LOCAL.get();
            return value == null ? "null" : value;
        }).get();

        result.put("nextValue", nextValue);
        result.put("note", "每个任务 finally 中 remove，可确保后续任务不会读到残留数据");
        return result;
    }

    /**
     * CompletableFuture 默认线程池不会继承 ThreadLocal。
     */
    public Map<String, Object> asyncContextDemo() throws InterruptedException, ExecutionException {
        Map<String, Object> result = new HashMap<>();
        DEMO_LOCAL.set("main-thread-value");

        // 默认 ForkJoinPool：读不到
        String defaultPoolValue = CompletableFuture.supplyAsync(() -> {
            String value = DEMO_LOCAL.get();
            return value == null ? "null" : value;
        }).get();

        // 手动包装：把主线程值拷贝到异步任务
        String copiedValue = CompletableFuture.supplyAsync(() -> {
            String value = DEMO_LOCAL.get();
            return value == null ? "null" : value;
        }, runnable -> {
            String value = DEMO_LOCAL.get();
            new Thread(() -> {
                DEMO_LOCAL.set(value);
                try {
                    runnable.run();
                } finally {
                    DEMO_LOCAL.remove();
                }
            }).start();
        }).get();

        DEMO_LOCAL.remove();
        result.put("defaultPoolValue", defaultPoolValue);
        result.put("manualCopyValue", copiedValue);
        result.put("note", "CompletableFuture 默认线程池不会继承 ThreadLocal；生产推荐用 TTL 或手动拷贝上下文");
        return result;
    }

    /**
     * TTL 线程池透传：Alibaba TransmittableThreadLocal + TtlExecutors。
     */
    public Map<String, Object> ttlPropagationDemo() throws InterruptedException, ExecutionException {
        Map<String, Object> result = new HashMap<>();
        TtlContext.set("ttl-value-from-main");

        String value = ttlExecutor.submit(() -> {
            String v = TtlContext.get();
            return v == null ? "null" : v;
        }).get();

        TtlContext.clear();
        result.put("transmittedValue", value);
        result.put("note", "TtlExecutors.getTtlExecutorService() 包装后，提交任务时会自动捕获并回放 ThreadLocal 上下文");
        return result;
    }
}

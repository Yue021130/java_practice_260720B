package com.example.async.controller;

import com.example.async.common.ApiResponse;
import com.example.async.support.CountingRejectedHandler;
import com.example.async.support.TraceContext;
import com.example.async.task.AsyncTaskService;
import com.example.async.task.BatchTaskService;
import com.example.async.task.SelfInvocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Tag(name = "Spring Boot 异步任务", description = "@Async、线程池、拒绝策略、异常、上下文透传、批量聚合等 14 个场景")
public class AsyncController {

    private final AsyncTaskService asyncTaskService;
    private final SelfInvocationService selfInvocationService;
    private final BatchTaskService batchTaskService;

    @Resource(name = "defaultPool")
    private ThreadPoolTaskExecutor defaultPool;

    @Resource(name = "cpuPool")
    private ThreadPoolTaskExecutor cpuPool;

    @Resource(name = "ioPool")
    private ThreadPoolTaskExecutor ioPool;

    @Resource(name = "rejectDemoPool")
    private ThreadPoolTaskExecutor rejectDemoPool;

    // ===================== 快速上手 =====================

    @PostMapping("/fire-forget")
    @Operation(summary = "无返回值 fire-and-forget", description = "@Async void 方法默认走默认线程池")
    public ApiResponse<Map<String, Object>> fireForget() {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        asyncTaskService.fireAndForget(taskId);

        // 稍微等待，让异步任务有机会把状态更新为 DONE
        sleepQuietly(120);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", asyncTaskService.getStatus(taskId));
        result.put("callerThread", Thread.currentThread().getName());
        result.put("interviewNote", "@Async 默认线程池是 SimpleAsyncTaskExecutor（每次新建线程），生产环境务必自定义 ThreadPoolTaskExecutor。");
        return ApiResponse.success(result);
    }

    @PostMapping("/completable-future")
    @Operation(summary = "CompletableFuture 返回值", description = "supplyAsync / thenApply / join 异步编排")
    public ApiResponse<Map<String, Object>> completableFuture() {
        long start = System.currentTimeMillis();
        CompletableFuture<Long> future = asyncTaskService.computeAsync(30)
                .thenApply(n -> n + 1);
        long value = future.join();
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> result = new HashMap<>();
        result.put("input", 30);
        result.put("output", value);
        result.put("costMs", cost);
        result.put("asyncThread", Thread.currentThread().getName()); // 当前已是异步线程 join 后的汇总线程
        result.put("interviewNote", "CompletableFuture 支持链式编排（thenApply/thenCompose/allOf/anyOf），join() 会阻塞等待结果；不要在 then 链里做阻塞 IO。");
        return ApiResponse.success(result);
    }

    @PostMapping("/future-timeout")
    @Operation(summary = "Future + 超时获取", description = "Future.get(timeout, TimeUnit) 避免永久阻塞")
    public ApiResponse<Map<String, Object>> futureTimeout() {
        Future<Integer> future = asyncTaskService.submitWithTimeout(8);
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();
        try {
            Integer value = future.get(500, TimeUnit.MILLISECONDS);
            result.put("value", value);
            result.put("timeout", false);
        } catch (TimeoutException e) {
            result.put("value", null);
            result.put("timeout", true);
            result.put("message", "500ms 内未拿到结果，避免永久阻塞");
        } catch (Exception e) {
            result.put("error", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
        result.put("costMs", System.currentTimeMillis() - start);
        result.put("interviewNote", "Future.get() 永久阻塞风险大，生产环境必须带超时：get(timeout, TimeUnit)；超时后考虑取消任务 future.cancel(true)。");
        return ApiResponse.success(result);
    }

    // ===================== 线程池配置 =====================

    @PostMapping("/pool-config")
    @Operation(summary = "ThreadPoolTaskExecutor 参数", description = "core/max/queue/keepAlive/rejection/shutdown 含义")
    public ApiResponse<Map<String, Object>> poolConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("cpuCores", Runtime.getRuntime().availableProcessors());
        result.put("defaultCore", defaultPool.getCorePoolSize());
        result.put("defaultMax", defaultPool.getMaxPoolSize());
        result.put("defaultQueueCapacity", defaultPool.getThreadPoolExecutor().getQueue().remainingCapacity()
                + defaultPool.getThreadPoolExecutor().getQueue().size());
        result.put("cpuCore", cpuPool.getCorePoolSize());
        result.put("cpuMax", cpuPool.getMaxPoolSize());
        result.put("ioCore", ioPool.getCorePoolSize());
        result.put("ioMax", ioPool.getMaxPoolSize());
        result.put("interviewNote", "corePoolSize 是常驻线程数；maxPoolSize 是队列满后扩容上限；queueCapacity 决定何时扩容；keepAliveTime 控制非核心线程存活；拒绝策略在 max+queue 都满时触发。");
        return ApiResponse.success(result);
    }

    @PostMapping("/custom-executor")
    @Operation(summary = "多线程池与 @Async(\"name\")", description = "按业务隔离线程池，避免相互挤占")
    public ApiResponse<Map<String, Object>> customExecutor() {
        String cpuThread = asyncTaskService.runOnCpuPool().join();
        String ioThread = asyncTaskService.runOnIoPool().join();

        Map<String, Object> result = new HashMap<>();
        result.put("cpuThread", cpuThread);
        result.put("ioThread", ioThread);
        result.put("cpuMatch", cpuThread.startsWith("cpu-pool-"));
        result.put("ioMatch", ioThread.startsWith("io-pool-"));
        result.put("interviewNote", "CPU 密集型任务用核心数固定的池，IO 密集型用更大的 maxPoolSize；通过 @Async(\"beanName\") 隔离不同业务，避免相互挤占。");
        return ApiResponse.success(result);
    }

    @PostMapping("/rejected")
    @Operation(summary = "队列打满与拒绝策略", description = "AbortPolicy / CallerRunsPolicy / DiscardPolicy / 自定义计数")
    public ApiResponse<Map<String, Object>> rejected() {
        CountingRejectedHandler handler = (CountingRejectedHandler) rejectDemoPool.getThreadPoolExecutor().getRejectedExecutionHandler();
        handler.getRejectedCount().set(0);

        int submitted = 0;
        int completed = 0;
        for (int i = 0; i < 20; i++) {
            try {
                rejectDemoPool.submit(() -> sleepQuietly(500));
                submitted++;
            } catch (RejectedExecutionException e) {
                // 计数已在 handler 中累加
            }
        }

        // 等待片刻，让已接收的任务有机会完成
        sleepQuietly(300);
        completed = (int) rejectDemoPool.getThreadPoolExecutor().getCompletedTaskCount();

        Map<String, Object> result = new HashMap<>();
        result.put("submitted", submitted);
        result.put("rejected", handler.getRejectedCount().get());
        result.put("completed", completed);
        result.put("active", rejectDemoPool.getActiveCount());
        result.put("queueSize", rejectDemoPool.getQueueSize());
        result.put("interviewNote", "AbortPolicy 抛异常；CallerRunsPolicy 让调用线程执行；DiscardPolicy 静默丢弃；DiscardOldestPolicy 丢弃最老任务。建议自定义计数器监控拒绝量。");
        return ApiResponse.success(result);
    }

    // ===================== 异常与代理 =====================

    @PostMapping("/exception")
    @Operation(summary = "异步异常处理", description = "AsyncUncaughtExceptionHandler、Future.exceptionally")
    public ApiResponse<Map<String, Object>> exception() {
        asyncTaskService.throwException();

        String recovered = asyncTaskService.throwInFuture()
                .exceptionally(ex -> "捕获异常:" + ex.getMessage())
                .join();

        sleepQuietly(50);

        Map<String, Object> result = new HashMap<>();
        result.put("voidExceptionHandledBy", "AsyncUncaughtExceptionHandler（查看日志）");
        result.put("futureRecovered", recovered);
        result.put("interviewNote", "void @Async 异常走 AsyncUncaughtExceptionHandler；有返回值的 Future/CompletableFuture 异常由调用方处理，如 exceptionally / handle / try-catch get()。");
        return ApiResponse.success(result);
    }

    @PostMapping("/self-invocation")
    @Operation(summary = "同类内部调用不生效", description = "Spring AOP 代理机制，this 调用绕过代理")
    public ApiResponse<Map<String, Object>> selfInvocation() {
        String pair = selfInvocationService.outer(); // outerThread|innerThread
        String[] parts = pair.split("\\|");

        Map<String, Object> result = new HashMap<>();
        result.put("outerThread", parts[0]);
        result.put("innerThread", parts[1]);
        result.put("sameThread", parts[0].equals(parts[1]));
        result.put("interviewNote", "同类内部 this.inner() 调用的是目标对象本身，不是 Spring 生成的代理，因此 @Async 注解不会生效；应注入自身代理或拆分到另一个 Bean。");
        return ApiResponse.success(result);
    }

    // ===================== 上下文透传 =====================

    @PostMapping("/context-propagation")
    @Operation(summary = "ThreadLocal / MDC 透传", description = "TaskDecorator 包装 Runnable，复制上下文到异步线程")
    public ApiResponse<Map<String, Object>> contextPropagation() {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        TraceContext.set(traceId);
        try {
            String asyncTraceId = asyncTaskService.readTraceIdAsync().join();
            Map<String, Object> result = new HashMap<>();
            result.put("mainTraceId", traceId);
            result.put("asyncTraceId", asyncTraceId);
            result.put("propagationOk", traceId.equals(asyncTraceId));
            result.put("interviewNote", "ThreadLocal 默认不会跨线程；使用 Spring TaskDecorator 包装 Runnable，在 run 前后 set/remove，可实现 traceId/MDC 透传，并避免线程池复用导致串号。");
            return ApiResponse.success(result);
        } finally {
            TraceContext.clear();
        }
    }

    // ===================== 生产场景 =====================

    @PostMapping("/batch-aggregate")
    @Operation(summary = "批量异步 + 结果聚合", description = "CompletableFuture.allOf / join / 汇总结果")
    public ApiResponse<Map<String, Object>> batchAggregate() {
        BatchTaskService.BatchResult batchResult = batchTaskService.runBatchAggregate();

        Map<String, Object> result = new HashMap<>();
        result.put("values", batchResult.getValues());
        result.put("sum", batchResult.getSum());
        result.put("costMs", batchResult.getCostMs());
        result.put("interviewNote", "批量任务用 CompletableFuture.allOf 等待全部完成，thenApply 汇总；避免在循环里逐个 get() 导致串行等待。");
        return ApiResponse.success(result);
    }

    @PostMapping("/controller-async")
    @Operation(summary = "异步 Controller", description = "Callable / CompletableFuture 释放 Tomcat 线程")
    public Callable<ApiResponse<Map<String, Object>>> controllerAsync() {
        String tomcatThread = Thread.currentThread().getName();
        return () -> {
            Map<String, Object> result = new HashMap<>();
            result.put("tomcatThread", tomcatThread);
            result.put("asyncThread", Thread.currentThread().getName());
            result.put("released", !tomcatThread.equals(Thread.currentThread().getName()));
            result.put("interviewNote", "返回 Callable / CompletableFuture 后，Spring MVC 会释放 Tomcat 线程，由任务线程异步处理，提升容器吞吐。");
            return ApiResponse.success(result);
        };
    }

    @PostMapping("/metrics")
    @Operation(summary = "线程池实时指标", description = "activeCount / queueSize / completedTaskCount / poolSize")
    public ApiResponse<Map<String, Object>> metrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("default", poolMetrics(defaultPool));
        result.put("cpu", poolMetrics(cpuPool));
        result.put("io", poolMetrics(ioPool));
        result.put("rejectDemo", poolMetrics(rejectDemoPool));
        result.put("interviewNote", "监控线程池要关注 activeCount、queueSize、completedTaskCount、poolSize、rejectedCount；一旦 queue 长期满载或 rejected 持续增长，应及时扩容或削峰。");
        return ApiResponse.success(result);
    }

    @PostMapping("/graceful-shutdown")
    @Operation(summary = "优雅关闭配置", description = "setWaitForTasksToCompleteOnShutdown + awaitTerminationSeconds")
    public ApiResponse<Map<String, Object>> gracefulShutdown() {
        Map<String, Object> result = new HashMap<>();
        result.put("defaultWaitForTasks", readWaitForTasks(defaultPool));
        result.put("defaultAwaitTerminationSeconds", readAwaitTerminationSeconds(defaultPool));
        result.put("cpuWaitForTasks", readWaitForTasks(cpuPool));
        result.put("ioWaitForTasks", readWaitForTasks(ioPool));
        result.put("rejectDemoWaitForTasks", readWaitForTasks(rejectDemoPool));
        result.put("interviewNote", "优雅关闭需同时设置 setWaitForTasksToCompleteOnShutdown(true) 和 setAwaitTerminationSeconds(N)，否则应用关闭时会直接丢弃队列中的任务。");
        return ApiResponse.success(result);
    }

    private boolean readWaitForTasks(ThreadPoolTaskExecutor pool) {
        try {
            java.lang.reflect.Field field = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                    .getDeclaredField("waitForTasksToCompleteOnShutdown");
            field.setAccessible(true);
            return (boolean) field.get(pool);
        } catch (Exception e) {
            return false;
        }
    }

    private int readAwaitTerminationSeconds(ThreadPoolTaskExecutor pool) {
        try {
            java.lang.reflect.Field field = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                    .getDeclaredField("awaitTerminationSeconds");
            field.setAccessible(true);
            return (int) field.get(pool);
        } catch (Exception e) {
            return -1;
        }
    }

    @PostMapping("/sync-vs-async")
    @Operation(summary = "异步 vs 同步对比", description = "同接口串行 vs 并行执行耗时对比")
    public ApiResponse<Map<String, Object>> syncVsAsync() {
        long syncCost = asyncTaskService.syncThreeTasks();
        long asyncCost = asyncTaskService.asyncThreeTasks();

        Map<String, Object> result = new HashMap<>();
        result.put("syncCostMs", syncCost);
        result.put("asyncCostMs", asyncCost);
        result.put("savedMs", syncCost - asyncCost);
        result.put("interviewNote", "3 个 200ms 的 IO 任务串行约 600ms，使用线程池并行后约 200ms；但线程池不是银弹，上下文切换和队列过长会反而降低吞吐。");
        return ApiResponse.success(result);
    }

    // ===================== 工具方法 =====================

    private Map<String, Object> poolMetrics(ThreadPoolTaskExecutor pool) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeCount", pool.getActiveCount());
        metrics.put("queueSize", pool.getQueueSize());
        metrics.put("completedTaskCount", pool.getThreadPoolExecutor().getCompletedTaskCount());
        metrics.put("poolSize", pool.getPoolSize());
        if (pool.getThreadPoolExecutor().getRejectedExecutionHandler() instanceof CountingRejectedHandler) {
            CountingRejectedHandler handler = (CountingRejectedHandler) pool.getThreadPoolExecutor().getRejectedExecutionHandler();
            metrics.put("rejectedCount", handler.getRejectedCount().get());
        }
        return metrics;
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

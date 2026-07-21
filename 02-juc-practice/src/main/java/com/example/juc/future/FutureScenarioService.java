package com.example.juc.future;

import com.example.juc.common.NamedThreadFactory;
import com.example.juc.common.ScenarioLog;
import com.example.juc.common.ScenarioResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 异步编排场景业务实现。
 *
 * 面试点：CompletableFuture 默认走 ForkJoinPool 公共池（生产要换自定义池）；
 * thenCompose 串行、thenCombine/applyToEither 并行；Future.get 阻塞不能链式；
 * ForkJoin 工作窃取；定时任务抛异常后静默停止。
 */
@Slf4j
@Service
public class FutureScenarioService {

    /**
     * CompletableFuture 电商详情页聚合。
     *
     * 八股：自定义线程池避免公共池被占满；exceptionally 做降级；Java 8 没有 orTimeout，
     * 超时兜底要用 applyToEither + ScheduledExecutor 实现。
     */
    public ScenarioResult cfDetail() {
        ScenarioLog log = new ScenarioLog();
        ExecutorService bizPool = Executors.newFixedThreadPool(8, new NamedThreadFactory("cf-biz-"));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new NamedThreadFactory("cf-sched-"));
        Map<String, Object> data = new HashMap<>();

        long t0 = System.currentTimeMillis();

        // 4 个远程服务并行：商品、库存、评价（抛异常降级）、推荐
        CompletableFuture<String> productCf = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            log.log("远程调用：商品信息返回");
            return "iPhone16";
        }, bizPool);

        CompletableFuture<String> stockCf = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            log.log("远程调用：库存返回");
            return "库存 100";
        }, bizPool);

        CompletableFuture<String> reviewsCf = CompletableFuture.<String>supplyAsync(() -> {
            sleep(500);
            log.log("远程调用：评价服务异常，触发 exceptionally 降级");
            throw new RuntimeException("评价服务超时");
        }, bizPool).exceptionally(ex -> {
            log.log("评价服务降级为默认好评");
            return "默认好评";
        });

        CompletableFuture<String> recommendCf = CompletableFuture.supplyAsync(() -> {
            sleep(400);
            log.log("远程调用：推荐返回");
            return "相似商品列表";
        }, bizPool);

        CompletableFuture<Void> all = CompletableFuture.allOf(productCf, stockCf, reviewsCf, recommendCf);
        all.join();
        long parallelElapsed = System.currentTimeMillis() - t0;
        log.log("allOf 聚合完成，并行总耗时 %dms（≈最慢服务 500ms，而非四个之和）", parallelElapsed);
        data.put("parallelElapsedMs", parallelElapsed);
        data.put("reviewsDegraded", true);

        // thenCompose：串行有依赖——查用户 → 查会员折扣
        CompletableFuture<String> discountCf = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            log.log("thenCompose：查询用户信息");
            return "user_123";
        }, bizPool).thenCompose(user -> CompletableFuture.supplyAsync(() -> {
            sleep(200);
            log.log("thenCompose：根据 %s 查询会员折扣", user);
            return user + ": 9折";
        }, bizPool));
        String composeResult = discountCf.join();
        log.log("thenCompose 串行结果：%s", composeResult);
        data.put("composeResult", composeResult);

        // applyToEither：两个镜像库存服务，取最快的
        CompletableFuture<String> stockA = CompletableFuture.supplyAsync(() -> {
            sleep(800);
            log.log("applyToEither：镜像库存 A 慢返回");
            return "stockA";
        }, bizPool);
        CompletableFuture<String> stockB = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            log.log("applyToEither：镜像库存 B 快返回");
            return "stockB";
        }, bizPool);
        String fastest = stockA.applyToEither(stockB, Function.identity()).join();
        log.log("applyToEither 取最快结果：%s", fastest);
        data.put("fastestResult", fastest);

        // Java 8 超时兜底：用 ScheduledExecutor 在 250ms 后 complete 一个默认值，与原任务 applyToEither 竞争
        CompletableFuture<String> slowService = CompletableFuture.supplyAsync(() -> {
            sleep(600);
            log.log("超时兜底：慢服务 600ms 后返回");
            return "慢服务结果";
        }, bizPool);
        CompletableFuture<String> timeoutFallback = new CompletableFuture<>();
        scheduler.schedule(() -> timeoutFallback.complete("兜底结果"), 250, TimeUnit.MILLISECONDS);
        String timeoutResult = slowService.applyToEither(timeoutFallback, Function.identity()).join();
        log.log("Java 8 超时兜底结果：%s（250ms 内未返回则走兜底）", timeoutResult);
        data.put("timeoutFallbackResult", timeoutResult);

        bizPool.shutdownNow();
        scheduler.shutdownNow();

        String summary = String.format("并行聚合耗时 %dms；CompletableFuture 用自定义池、exceptionally 降级、applyToEither 竞速、Scheduled 实现超时兜底",
                parallelElapsed);
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * 老式 Future 串行写法。
     *
     * 八股：Future.get 阻塞；不能链式组合；所以 Java 8 引入 CompletableFuture。
     */
    public ScenarioResult oldFuture() {
        ScenarioLog log = new ScenarioLog();
        ExecutorService pool = Executors.newFixedThreadPool(4, new NamedThreadFactory("old-future-"));
        long t0 = System.currentTimeMillis();

        try {
            Future<String> product = pool.submit(new RemoteTask("商品", 300, log));
            Future<String> stock = pool.submit(new RemoteTask("库存", 200, log));
            Future<String> reviews = pool.submit(new RemoteTask("评价", 500, log));
            Future<String> recommend = pool.submit(new RemoteTask("推荐", 400, log));

            String p = product.get();
            String s = stock.get();
            String r = reviews.get();
            String rec = recommend.get();
            long elapsed = System.currentTimeMillis() - t0;
            log.log("老式 Future 串行 get 汇总：%s %s %s %s，总耗时 %dms", p, s, r, rec, elapsed);

            Map<String, Object> data = new HashMap<>();
            data.put("serialElapsedMs", elapsed);
            data.put("result", p + " " + s + " " + r + " " + rec);
            return ScenarioResult.of(log, "老式 Future 总耗时 ≈ 各服务之和，无法声明式编排", data);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return ScenarioResult.of(log, "老式 Future 执行异常：" + e.getMessage(), new HashMap<>());
        } finally {
            pool.shutdownNow();
        }
    }

    private static class RemoteTask implements Callable<String> {
        private final String name;
        private final int ms;
        private final ScenarioLog log;
        RemoteTask(String name, int ms, ScenarioLog log) {
            this.name = name;
            this.ms = ms;
            this.log = log;
        }
        @Override
        public String call() {
            sleep(ms);
            log.log("老式 Future %s 返回", name);
            return name;
        }
    }

    /**
     * ForkJoin 分治求和。
     *
     * 八股：工作窃取 work-stealing；阈值太大并行度低，太小分治开销反超；
     * parallelStream 共用公共 ForkJoinPool，IO 任务会拖累。
     */
    public ScenarioResult forkjoinSum(int size) {
        ScenarioLog log = new ScenarioLog();
        long[] arr = new long[size];

        long initT0 = System.nanoTime();
        for (int i = 0; i < size; i++) {
            arr[i] = i + 1;
        }
        long initElapsed = (System.nanoTime() - initT0) / 1_000_000;
        log.log("数组初始化完成，规模 %d，耗时 %dms", size, initElapsed);

        ForkJoinPool pool = new ForkJoinPool();
        long t0 = System.nanoTime();
        ForkJoinTask<Long> task = new SumTask(arr, 0, size, 10_000_000);
        long forkJoinResult = pool.invoke(task);
        long forkJoinElapsed = (System.nanoTime() - t0) / 1_000_000;
        log.log("ForkJoin 求和结果 %d，耗时 %dms", forkJoinResult, forkJoinElapsed);

        long t1 = System.nanoTime();
        long serialResult = 0;
        for (long v : arr) serialResult += v;
        long serialElapsed = (System.nanoTime() - t1) / 1_000_000;
        log.log("单线程求和结果 %d，耗时 %dms", serialResult, serialElapsed);

        pool.shutdown();

        Map<String, Object> data = new HashMap<>();
        data.put("size", size);
        data.put("initElapsedMs", initElapsed);
        data.put("forkJoinResult", forkJoinResult);
        data.put("forkJoinElapsedMs", forkJoinElapsed);
        data.put("serialResult", serialResult);
        data.put("serialElapsedMs", serialElapsed);
        data.put("resultEqual", forkJoinResult == serialResult);
        double speedup = serialElapsed > 0 ? (double) serialElapsed / Math.max(1, forkJoinElapsed) : 0;
        data.put("speedup", String.format("%.1fx", speedup));
        return ScenarioResult.of(log, String.format("ForkJoin 并行 %.1f 倍速，结果一致", speedup), data);
    }

    private static class SumTask extends RecursiveTask<Long> {
        private final long[] arr;
        private final int start;
        private final int end;
        private final int threshold;
        SumTask(long[] arr, int start, int end, int threshold) {
            this.arr = arr;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }
        @Override
        protected Long compute() {
            if (end - start <= threshold) {
                long sum = 0;
                for (int i = start; i < end; i++) sum += arr[i];
                return sum;
            }
            int mid = (start + end) >>> 1;
            SumTask left = new SumTask(arr, start, mid, threshold);
            SumTask right = new SumTask(arr, mid, end, threshold);
            left.fork();
            long rightSum = right.compute();
            long leftSum = left.join();
            return leftSum + rightSum;
        }
    }

    /**
     * 定时任务两种模式对比 + 异常后静默停止。
     *
     * 八股：FixedRate 以上次开始为基准可能追赶；FixedDelay 以上次结束顺延；
     * 任务抛异常且未捕获后续调度全部静默停止。
     */
    public ScenarioResult scheduledCompare() {
        ScenarioLog log = new ScenarioLog();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new NamedThreadFactory("sched-"));
        Map<String, Object> data = new HashMap<>();

        List<Long> rateTimes = new ArrayList<>();
        List<Long> delayTimes = new ArrayList<>();
        long base = System.currentTimeMillis();

        ScheduledFuture<?> rateFuture = scheduler.scheduleAtFixedRate(() -> {
            rateTimes.add(System.currentTimeMillis() - base);
            log.log("FixedRate 执行，距开始 %dms", System.currentTimeMillis() - base);
            sleep(300);
        }, 0, 500, TimeUnit.MILLISECONDS);

        ScheduledFuture<?> delayFuture = scheduler.scheduleWithFixedDelay(() -> {
            delayTimes.add(System.currentTimeMillis() - base);
            log.log("FixedDelay 执行，距开始 %dms", System.currentTimeMillis() - base);
            sleep(300);
        }, 0, 500, TimeUnit.MILLISECONDS);

        try {
            Thread.sleep(3200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        rateFuture.cancel(false);
        delayFuture.cancel(false);

        List<Long> rateIntervals = intervals(rateTimes);
        List<Long> delayIntervals = intervals(delayTimes);
        data.put("rateIntervals", rateIntervals);
        data.put("delayIntervals", delayIntervals);
        log.log("FixedRate 间隔 %s（保持≈周期 500ms）", rateIntervals);
        log.log("FixedDelay 间隔 %s（≈周期+执行时间 800ms，顺延）", delayIntervals);

        // 异常后静默停止
        AtomicInteger count = new AtomicInteger();
        ScheduledFuture<?> errFuture = scheduler.scheduleWithFixedDelay(() -> {
            int c = count.incrementAndGet();
            log.log("异常任务第 %d 次执行", c);
            if (c == 3) {
                throw new RuntimeException("故意抛异常");
            }
        }, 0, 300, TimeUnit.MILLISECONDS);

        try {
            Thread.sleep(1800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean stopped = errFuture.isDone() || errFuture.isCancelled();
        data.put("silentStop", stopped);
        log.log("异常任务第三次抛异常后，调度静默停止：isDone=%s isCancelled=%s", errFuture.isDone(), errFuture.isCancelled());

        scheduler.shutdownNow();
        return ScenarioResult.of(log, "FixedRate 追赶 vs FixedDelay 顺延；定时任务体必须 try-catch，否则异常后无声消失", data);
    }

    private List<Long> intervals(List<Long> times) {
        List<Long> list = new ArrayList<>();
        for (int i = 1; i < times.size(); i++) {
            list.add(times.get(i) - times.get(i - 1));
        }
        return list;
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

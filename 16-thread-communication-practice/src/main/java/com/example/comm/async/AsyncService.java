package com.example.comm.async;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * 08. 基于异步结果传递：Future / FutureTask + CompletableFuture。
 *
 * - FutureTask：把「计算」包装成任务，跨线程把返回值传回来，get() 阻塞等结果；
 * - CompletableFuture：Future 的完整进化版——回调编排、多任务组合（allOf/anyOf）、
 *   链式依赖、异常处理，是现代 Java 异步通信的首选。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncService {

    private final CommLogStore logStore;

    @Resource(name = "cachedPool")
    private ExecutorService cachedPool;

    /**
     * FutureTask：提交一个计算任务，主线程 get() 阻塞等结果。
     * 演示「计算在线程里跑、结果跨线程传回来」。
     */
    public Map<String, Object> futureDemo(int taskMs) {
        int safeTaskMs = Math.max(20, Math.min(taskMs, 5000));
        Map<String, Object> result = new LinkedHashMap<>();

        FutureTask<String> task = new FutureTask<>(() -> {
            sleep(safeTaskMs);
            return "计算完成，耗时 " + safeTaskMs + "ms";
        });
        Thread worker = new Thread(task, "future-worker");
        worker.start();

        long start = System.nanoTime();
        String value;
        try {
            value = task.get();          // 阻塞，直到任务返回结果
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            value = "获取失败：" + e.getMessage();
        }
        long getMs = (System.nanoTime() - start) / 1_000_000;

        result.put("taskMs", safeTaskMs);
        result.put("result", value);
        result.put("getBlockMs", getMs);
        result.put("done", task.isDone());
        result.put("tip", "结果「" + value + "」由 worker 线程跨线程传回，主线程 get() 阻塞等了 " + getMs
                + "ms：Future 就是「异步计算 + 阻塞取结果」的最小形态。");

        logStore.add("async", "future-demo", 1, task.isDone(), "FutureTask");
        return result;
    }

    /**
     * CompletableFuture 链式编排：一个任务完成后自动接下一个，不用手动阻塞。
     */
    public Map<String, Object> cfDemo(int taskMs) {
        int safeTaskMs = Math.max(20, Math.min(taskMs, 5000));
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.nanoTime();

        // 查询原始数据 → 加工 → 消费，三步自动串起来
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            sleep(safeTaskMs);
            return "原始数据";
        }, cachedPool)
                .thenApply(data -> data + " → 加工")        // 上一步结果作为入参
                .thenApply(data -> data + " → 包装")
                .thenApplyAsync(data -> data + " → 异步再加工", cachedPool);

        String finalValue = future.join();   // 等待整条链完成（也可以不 join 直接回调）
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("taskMs", safeTaskMs);
        result.put("pipeline", new String[]{"supplyAsync(查原始数据)", "thenApply(加工)", "thenApply(包装)", "thenApplyAsync(异步再加工)"});
        result.put("finalResult", finalValue);
        result.put("totalMs", totalMs);
        result.put("tip", "「" + finalValue + "」由 4 个阶段自动衔接完成，耗时 " + totalMs + "ms："
                + "thenApply 等上一步返回后自动执行，全程无需手写线程同步。");

        logStore.add("async", "cf-demo", 1, true, "CompletableFuture 链式");
        return result;
    }

    /**
     * allOf / anyOf 组合 + exceptionally 异常兜底。
     */
    public Map<String, Object> cfCombine(int tasks) {
        int safeTasks = Math.max(2, Math.min(tasks, 8));
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. allOf：等全部任务完成
        List<CompletableFuture<String>> all = new ArrayList<>();
        for (int i = 0; i < safeTasks; i++) {
            final int id = i;
            all.add(CompletableFuture.supplyAsync(() -> {
                sleep(30 + id * 10);
                return "任务" + id + "完成";
            }, cachedPool));
        }
        long allStart = System.nanoTime();
        CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).join();
        long allMs = (System.nanoTime() - allStart) / 1_000_000;
        List<String> allResults = new ArrayList<>();
        all.forEach(f -> allResults.add(f.join()));

        // 2. anyOf：任一先完成即可（最快的那个）
        long anyStart = System.nanoTime();
        Object first = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(80); return "慢任务"; }, cachedPool),
                CompletableFuture.supplyAsync(() -> { sleep(20); return "快任务"; }, cachedPool)
        ).join();
        long anyMs = (System.nanoTime() - anyStart) / 1_000_000;

        // 3. exceptionally：失败兜底
        String fallback = CompletableFuture.<String>supplyAsync(() -> {
            throw new IllegalStateException("模拟失败");
        }, cachedPool)
                .exceptionally(ex -> "兜底结果（" + ex.getCause().getMessage() + "）")
                .join();

        result.put("tasks", safeTasks);
        result.put("allOfWaitMs", allMs);
        result.put("allResults", allResults);
        result.put("anyOfFirst", first);
        result.put("anyOfWaitMs", anyMs);
        result.put("exceptionallyResult", fallback);
        result.put("tip", "allOf 等 " + safeTasks + " 个任务全完成（" + allMs + "ms），anyOf 只要最快的「" + first + "」（" + anyMs
                + "ms），exceptionally 把失败兜成「" + fallback + "」：组合与容错一次到位。");

        logStore.add("async", "cf-combine", safeTasks, true, "allOf/anyOf");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("futureLimit", new String[]{
                "get() 阻塞：等结果期间当前线程干不了别的",
                "无法编排：不能把一个任务的结果直接喂给下一个任务",
                "无法组合：allOf / anyOf 这种「等一批」只能自己用 CountDownLatch 拼",
                "异常不友好：ExecutionException 包一层，排查麻烦"
        });
        result.put("completableFuture", new LinkedHashMap<String, Object>() {{
            put("编排", "thenApply（转换）/ thenAccept（消费）/ thenCompose（异步串联）/ whenComplete（收尾）");
            put("组合", "allOf（等全部）/ anyOf（任一完成）");
            put("容错", "exceptionally（兜底）/ handle（成功失败都处理）/ completeExceptionally（手动失败）");
            put("线程池", "不传池子用 ForkJoinPool.commonPool；生产务必传独立线程池，避免 IO 阻塞拖垮公共池");
            put("注意", "thenApply 在哪个线程跑取决于上一步完成线程与调用线程（async 后缀强制入池）；别在回调里做重活");
        }});
        result.put("whenToUse", "要「拿单个结果」用 Future；要「编排多个异步流程」用 CompletableFuture；"
                + "再复杂就上响应式（Project Reactor / RxJava）。");
        result.put("tip", "面试：先讲 Future 三个痛点（阻塞、不能串、不能并），再讲 CompletableFuture 四板斧（thenApply/allOf/anyOf/exceptionally）。");
        return result;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package com.example.comm.shared;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 01. 基于共享内存：volatile 可见性 + 原子类 CAS。
 *
 * 线程间通信最基础的一类——不依赖任何锁/队列，直接共享变量：
 * - volatile：保证「可见性」+「禁止指令重排」，但不保证「原子性」；
 * - AtomicXxx：CAS 无锁更新，原子且可见，适合计数、状态切换。
 *
 * 一句话记忆：volatile 解决「看得见」，CAS 解决「改得对」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharedService {

    private final CommLogStore logStore;

    /**
     * volatile 可见性：主线程把标志位写进主存，N 个 worker 轮询感知。
     *
     * 每个 worker 死循环读 {@code ready}，直到可见才退出，并记录自己
     * 「从启动到看到标志」的延迟——如果没有 volatile，JIT 可能把读缓存到
     * 寄存器，worker 永远看不到更新。
     */
    public Map<String, Object> volatileDemo(int workers, int flagDelayMs) {
        int safeWorkers = Math.max(1, Math.min(workers, 32));
        Map<String, Object> result = new LinkedHashMap<>();

        // 没有 volatile 的话，worker 的 while(!ready) 可能永远跳不出去
        Holder holder = new Holder();
        CountDownLatch allPerceived = new CountDownLatch(safeWorkers);
        List<Long> latencies = new CopyOnWriteArrayList<>();

        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < safeWorkers; i++) {
            Thread t = new Thread(() -> {
                long t0 = System.nanoTime();
                // 轮询：读 volatile 字段
                while (!holder.ready) {
                    // 空转等待，模拟「检查到 true 才干活」
                }
                latencies.add((System.nanoTime() - t0) / 1_000_000);
                allPerceived.countDown();
            }, "volatile-worker-" + i);
            threads.add(t);
            t.start();
        }

        // 主线程睡一会，确认 worker 都已进入轮询，再置位
        sleep(flagDelayMs);
        holder.ready = true;   // 写 volatile，对其它线程立即可见
        boolean allDone = false;
        try {
            allDone = allPerceived.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("workers", safeWorkers);
        result.put("flagDelayMs", flagDelayMs);
        result.put("allPerceived", allDone);
        result.put("perceivedCount", safeWorkers - (int) allPerceived.getCount());
        result.put("maxLatencyMs", latencies.isEmpty() ? 0 : latencies.stream().mapToLong(Long::longValue).max().orElse(0));
        result.put("totalMs", totalMs);
        result.put("tip", "置位后 N 个 worker 全部感知到（perceivedCount=" + (safeWorkers - (int) allPerceived.getCount())
                + "），说明 volatile 保证可见性；若去掉 volatile，JIT 缓存可能让 worker 永远等不到。");
        // 收尾：让还在轮询的线程退出
        holder.ready = true;

        logStore.add("shared", "volatile-demo", safeWorkers, allDone, "volatile 可见性");
        return result;
    }

    /** 简单 volatile 持有者，避免数组元素 volatile 的别扭写法 */
    private static class Holder {
        volatile boolean ready = false;
    }

    /**
     * 原子类 CAS：N 个线程各累加 increments 次。
     *
     * 普通 int 的 {@code ++} 是「读-改-写」三步，非原子，并发下会互相覆盖丢数；
     * AtomicInteger 的 incrementAndGet 走 CAS 自旋，最终值一定精确。
     */
    public Map<String, Object> atomicDemo(int threads, int increments) {
        int safeThreads = Math.max(1, Math.min(threads, 64));
        int safeIncrements = Math.max(1, Math.min(increments, 100_000));
        Map<String, Object> result = new LinkedHashMap<>();

        int[] plain = {0};                                  // 非原子累加
        AtomicInteger atomic = new AtomicInteger(0);        // CAS 累加
        CountDownLatch done = new CountDownLatch(safeThreads);

        long start = System.nanoTime();
        for (int i = 0; i < safeThreads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < safeIncrements; j++) {
                    plain[0]++;                             // 可能丢更新
                    atomic.incrementAndGet();               // CAS 保证不丢
                }
                done.countDown();
            }, "atomic-worker-" + i);
            t.start();
        }
        try {
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;
        int expected = safeThreads * safeIncrements;

        result.put("threads", safeThreads);
        result.put("increments", safeIncrements);
        result.put("expected", expected);
        result.put("plainIntResult", plain[0]);
        result.put("atomicResult", atomic.get());
        result.put("plainLost", expected - plain[0]);
        result.put("totalMs", totalMs);
        result.put("tip", "plain 只有 " + plain[0] + "（丢了 " + (expected - plain[0]) + " 次更新），"
                + "AtomicInteger 精确到 " + atomic.get() + "：CAS 用「比较-交换」的自旋把读改写变成原子操作。");

        logStore.add("shared", "atomic-demo", safeThreads, atomic.get() == expected, "CAS 原子性");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("volatile", new LinkedHashMap<String, Object>() {{
            put("可见性", "线程写 volatile 变量会立刻刷主存，其它线程读时强制读主存，杜绝各自缓存里的旧值");
            put("有序性", "禁止重排序：读/写 volatile 前后不能乱序（半同步：LoadLoad/StoreStore 屏障）");
            put("不保证原子性", "i++ 这种「读-改-写」三步不是原子操作，并发下仍会丢更新，计数必须用原子类或锁");
            put("适用", "状态标志位 / 双重检查锁的单例字段 / 发布不可变对象");
        }});
        result.put("cas", new LinkedHashMap<String, Object>() {{
            put("原理", "CAS(Compare And Swap)：读旧值 V、比较是否仍是 V、是则交换为新值，不是则自旋重试，全程无锁");
            put("底层", "CPU 指令（x86 的 cmpxchg）+ 处理器总线锁/缓存锁，保证比较-交换原子");
            put("ABA 问题", "A→B→A 中间被改过但比较仍相等；可用 AtomicStampedReference（带版本号）解决");
            put("适用", "计数、状态切换、并发容器（ConcurrentHashMap 内部大量用 CAS）");
            put("代价", "高竞争下自旋浪费 CPU，活锁风险；「改得复杂」时锁反而更简单");
        }});
        result.put("relation", "volatile 解决「看得见」（可见性），CAS 解决「改得对」（原子性）；两者都无锁、都轻量，是共享内存通信的两块基石。");
        result.put("tip", "面试答「线程间通信」先讲这一层：最基础的是共享变量，但要用对工具（volatile 不能替代原子类）。");
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

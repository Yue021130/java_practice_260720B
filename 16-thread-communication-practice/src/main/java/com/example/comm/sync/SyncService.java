package com.example.comm.sync;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 06. JUC 同步工具：基于 AQS 封装的高层通信语义。
 *
 * - CountDownLatch：一个线程等 N 个线程完成（一次性）；
 * - CyclicBarrier：N 个线程互相等，到齐放行（可复用）；
 * - Semaphore：限流，控制同时进入的线程数；
 * - Exchanger：两个线程碰头双向交换数据；
 * - Phaser：Latch + Barrier 合体，支持多阶段 + 动态增减参与者。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final CommLogStore logStore;

    /**
     * CountDownLatch：主线程 await，等 N 个 worker 各自 countDown。
     * 典型的「一等多」：分发任务 → 主线程阻塞 → 全部完成后继续。
     */
    public Map<String, Object> latchDemo(int workers) {
        int safeWorkers = Math.max(1, Math.min(workers, 16));
        Map<String, Object> result = new LinkedHashMap<>();
        CountDownLatch latch = new CountDownLatch(safeWorkers);

        long start = System.nanoTime();
        for (int i = 0; i < safeWorkers; i++) {
            Thread t = new Thread(() -> {
                sleep(60);            // 模拟干活
                latch.countDown();    // 干完一个就减一
            }, "latch-worker-" + i);
            t.start();
        }
        try {
            latch.await();           // 主线程阻塞，直到计数归零
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("workers", safeWorkers);
        result.put("latchCountAfter", latch.getCount());
        result.put("totalMs", totalMs);
        result.put("reusable", false);
        result.put("tip", "主线程 await 后等全部 " + safeWorkers + " 个 worker countDown，总共花了 " + totalMs
                + "ms（≈最慢那个 worker 的 60ms）。Latch 是一次性的：计数到 0 后不能再复用。");

        logStore.add("sync", "latch-demo", safeWorkers, true, "CountDownLatch");
        return result;
    }

    /**
     * CyclicBarrier：parties 个线程每轮都「到齐才放行」，循环 rounds 轮。
     * 与 Latch 的核心区别：可重复使用，且是「N 等 N」互相等。
     */
    public Map<String, Object> barrierDemo(int parties, int rounds) {
        int safeParties = Math.max(2, Math.min(parties, 16));
        int safeRounds = Math.max(1, Math.min(rounds, 10));
        Map<String, Object> result = new LinkedHashMap<>();
        AtomicInteger completedRounds = new AtomicInteger();
        List<Integer> roundTimes = new ArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(safeParties, () -> {
            // 每轮所有参与者到齐后，由「最后一个到达的线程」执行这个动作
            completedRounds.incrementAndGet();
        });

        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < safeParties; i++) {
            Thread t = new Thread(() -> {
                try {
                    for (int r = 0; r < safeRounds; r++) {
                        long t0 = System.nanoTime();
                        sleep(20 + r * 10);            // 各参与者耗时不同，先到的要等后到的
                        barrier.await();               // 到齐才放行
                        roundTimes.add((int) ((System.nanoTime() - t0) / 1_000_000));
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }, "barrier-" + i);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("parties", safeParties);
        result.put("rounds", safeRounds);
        result.put("completedRounds", completedRounds.get());
        result.put("totalMs", totalMs);
        result.put("reusable", true);
        result.put("tip", safeParties + " 个线程每轮「到齐才放行」，循环了 " + safeRounds
                + " 轮、全部 " + completedRounds.get() + " 轮完整走完：Barrier 可复用，先到的等待后到的，"
                + "每轮由最后到的人触发 barrierAction。");

        logStore.add("sync", "barrier-demo", safeParties, completedRounds.get() == safeRounds, "CyclicBarrier");
        return result;
    }

    /**
     * Semaphore：同时最多 permits 个线程进入临界区（限流/资源池）。
     * 记录并发峰值 ≤ permits。
     */
    public Map<String, Object> semaphoreDemo(int permits, int threads) {
        int safePermits = Math.max(1, Math.min(permits, 16));
        int safeThreads = Math.max(1, Math.min(threads, 64));
        Map<String, Object> result = new LinkedHashMap<>();
        Semaphore sem = new Semaphore(safePermits);
        AtomicInteger current = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(safeThreads);

        long start = System.nanoTime();
        for (int i = 0; i < safeThreads; i++) {
            Thread t = new Thread(() -> {
                try {
                    sem.acquire();                       // 拿许可，拿不到就阻塞排队
                    int c = current.incrementAndGet();
                    peak.accumulateAndGet(c, Math::max);
                    sleep(30);                           // 在临界区里干活
                    current.decrementAndGet();
                    sem.release();                       // 还许可
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "sem-worker-" + i);
            t.start();
        }
        try {
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("permits", safePermits);
        result.put("threads", safeThreads);
        result.put("peakConcurrent", peak.get());
        result.put("totalMs", totalMs);
        result.put("tip", "许可=" + safePermits + "，并发峰值=" + peak.get() + "（≤许可数）："
                + safeThreads + " 个线程抢 " + safePermits + " 个许可，同时进入的永远不会超过 " + safePermits + "，这就是限流。");

        logStore.add("sync", "semaphore-demo", safeThreads, peak.get() <= safePermits, "Semaphore 限流");
        return result;
    }

    /**
     * Exchanger：两个线程在交换点碰头，双向交换数据后各自继续。
     */
    public Map<String, Object> exchangerDemo() {
        Map<String, Object> result = new LinkedHashMap<>();
        Exchanger<String> exchanger = new Exchanger<>();
        String[] aReceived = {""};
        String[] bReceived = {""};

        Thread a = new Thread(() -> {
            try {
                aReceived[0] = exchanger.exchange("A 的包裹");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "exchanger-A");
        Thread b = new Thread(() -> {
            try {
                bReceived[0] = exchanger.exchange("B 的包裹");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "exchanger-B");

        long start = System.nanoTime();
        a.start();
        b.start();
        try {
            a.join();
            b.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("aSent", "A 的包裹");
        result.put("bSent", "B 的包裹");
        result.put("aReceived", aReceived[0]);
        result.put("bReceived", bReceived[0]);
        result.put("swapped", "A 的包裹".equals(bReceived[0]) && "B 的包裹".equals(aReceived[0]));
        result.put("totalMs", totalMs);
        result.put("tip", "两个线程必须同时到达 exchange() 才会完成交换（互相等），耗时 "
                + totalMs + "ms。A 收到「" + aReceived[0] + "」，B 收到「" + bReceived[0] + "」：数据双向交换成功。");

        logStore.add("sync", "exchanger-demo", 2, result.get("swapped").equals(true), "Exchanger");
        return result;
    }

    /**
     * Phaser：多阶段同步 + 动态增减参与者。
     * 演示：初始 parties 个参与者，中途 register 一个新参与者并再 arriveAndDeregister，
     * 走 3 个 phase，每个 phase 都要求当前所有参与者到齐。
     */
    public Map<String, Object> phaserDemo(int parties) {
        int safeParties = Math.max(2, Math.min(parties, 16));
        Map<String, Object> result = new LinkedHashMap<>();
        Phaser phaser = new Phaser(safeParties);      // 初始注册 parties 个
        List<Integer> phaseSizes = new ArrayList<>();
        AtomicInteger phaseCount = new AtomicInteger();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < safeParties; i++) {
            Thread t = new Thread(() -> {
                try {
                    for (int phase = 0; phase < 3; phase++) {
                        sleep(10 + phase * 15);
                        phaser.arriveAndAwaitAdvance();   // 到齐才进入下一阶段
                        if (phase == 2) {
                            phaseSizes.add(phaser.getRegisteredParties());
                        }
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }, "phaser-" + i);
            threads.add(t);
            t.start();
        }

        // 中途动态注册一个「临时工」：干完一阶段后 arriveAndDeregister 注销
        Thread temp = new Thread(() -> {
            try {
                phaser.register();                        // 动态增加参与者
                phaseCount.set(phaser.getPhase());
                sleep(15);
                phaser.arriveAndAwaitAdvance();
                phaser.arriveAndDeregister();             // 干完注销，不再参与后续阶段
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, "phaser-temp");
        temp.start();

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            temp.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        result.put("initialParties", safeParties);
        result.put("registeredNow", phaser.getRegisteredParties());
        result.put("phase", phaser.getPhase());
        result.put("tempRegisteredAtPhase", phaseCount.get());
        result.put("tip", "Phaser 初始 " + safeParties + " 个参与者，中途 register 一个临时工、干完再 "
                + "arriveAndDeregister（注册数从 " + (safeParties + 1) + " 回到 " + safeParties + "）："
                + "多阶段 + 动态增减，是 Latch 和 Barrier 的合体。");

        logStore.add("sync", "phaser-demo", safeParties, true, "Phaser 多阶段");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compare", new LinkedHashMap<String, Object>() {{
            put("CountDownLatch", "一等多（主线程等 N 个 worker）；countDown 减计数、await 等归零；一次性；不可重置");
            put("CyclicBarrier", "N 等 N（互相等齐）；await 到齐放行；可 reset 循环复用；有 barrierAction 回调");
            put("Semaphore", "限流/资源池；acquire 拿许可、release 还许可；可公平/非公平；控制并发数");
            put("Exchanger", "两个线程碰头；exchange 双向交换；成对出现，用于「校对/交换缓冲区」");
            put("Phaser", "Latch+Barrier 合体；多阶段；register/arriveAndDeregister 动态增减；可树形分组");
        }});
        result.put("latchVsBarrier", new LinkedHashMap<String, Object>() {{
            put("方向", "Latch 是「一个等 N 个」；Barrier 是「N 个互相等」");
            put("复用", "Latch 一次性（计数到 0 报废）；Barrier 可重复（reset / 自动循环）");
            put("典型", "Latch：网关等所有服务就绪、并发压测启动信号；Barrier：分页抓取到齐合并、多线程计算到齐归约");
        }});
        result.put("aqs", "这五个工具内部都基于 AQS：Latch 用共享锁计数、Semaphore 用共享锁管理 permits、"
                + "Barrier/Exchanger/Phaser 基于 Condition 或 AQS 状态机。底层又都是「阻塞 + 等待队列 + 唤醒」");
        result.put("tip", "面试：把对比表背下来，再补一句「Latch 一次性 vs Barrier 循环、Latch 一等多 vs Barrier 多等多」就是标准答案。");
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

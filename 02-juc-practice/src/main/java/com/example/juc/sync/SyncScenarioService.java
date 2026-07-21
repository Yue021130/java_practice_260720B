package com.example.juc.sync;

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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 同步工具场景业务实现。
 *
 * 面试点：CountDownLatch 一次性、CyclicBarrier 可复用且带回调、Semaphore 是限流/连接池本质、
 * Exchanger 必须成双成对。
 */
@Slf4j
@Service
public class SyncScenarioService {

    /**
     * CountDownLatch 并行启动自检。
     *
     * 八股：一次性不可复用；state 即计数，归零唤醒所有 await 线程；countDown 可在任意线程调用。
     */
    public ScenarioResult latchStartup() {
        ScenarioLog log = new ScenarioLog();
        Map<String, Object> data = new HashMap<>();

        // 正常启动：3 个依赖全部就绪
        CountDownLatch latch = new CountDownLatch(3);
        String[] deps = {"db-check", "redis-check", "mq-check"};
        for (String dep : deps) {
            Thread t = new Thread(() -> {
                long ms = ThreadLocalRandom.current().nextInt(500, 1500);
                try {
                    Thread.sleep(ms);
                    log.log("%s 完成（耗时 %dms），countDown", dep, ms);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, dep);
            t.setDaemon(true);
            t.start();
        }

        log.log("main 等待全部依赖就绪…");
        long t0 = System.currentTimeMillis();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long startupMs = System.currentTimeMillis() - t0;
        log.log("main：3/3 全部就绪，开始接收流量，总耗时 %dms", startupMs);
        data.put("startupMs", startupMs);
        data.put("readyCount", 3);

        // 超时场景：只就绪 2/3
        CountDownLatch latch2 = new CountDownLatch(3);
        for (int i = 0; i < 2; i++) {
            final String dep = deps[i];
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(200);
                    log.log("%s 完成，countDown", dep);
                    latch2.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, dep + "-timeout");
            t.setDaemon(true);
            t.start();
        }
        try {
            boolean ok = latch2.await(2, TimeUnit.SECONDS);
            data.put("timeoutCaseReady", 2);
            log.log("main：await 超时返回 %s，仅就绪 2/3，拒绝接收流量", ok);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ScenarioResult.of(log, "CountDownLatch 一次性计数：全部就绪才放行，超时则启动失败", data);
    }

    /**
     * CyclicBarrier 分片报表汇总。
     *
     * 八股：与 CountDownLatch 三区别——可复用、计数归参与线程、支持到达回调 barrierAction。
     */
    public ScenarioResult barrierReport(int rounds) {
        ScenarioLog log = new ScenarioLog();
        int parties = 4;
        AtomicInteger[] quarterSums = new AtomicInteger[parties];
        for (int i = 0; i < parties; i++) {
            quarterSums[i] = new AtomicInteger();
        }
        AtomicInteger yearTotal = new AtomicInteger();

        CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
            int total = 0;
            for (AtomicInteger q : quarterSums) {
                total += q.get();
            }
            yearTotal.set(total);
            log.log("barrierAction：Q1=%d Q2=%d Q3=%d Q4=%d 年报汇总=%d",
                    quarterSums[0].get(), quarterSums[1].get(), quarterSums[2].get(), quarterSums[3].get(), total);
        });

        ExecutorService pool = Executors.newFixedThreadPool(parties, new NamedThreadFactory("barrier-"));
        CountDownLatch done = new CountDownLatch(parties);

        for (int i = 0; i < parties; i++) {
            final int quarter = i;
            pool.submit(() -> {
                try {
                    for (int r = 0; r < rounds; r++) {
                        int sales = ThreadLocalRandom.current().nextInt(100, 500);
                        quarterSums[quarter].set(sales);
                        log.log("Q%d 线程第 %d 轮销售额=%d万，等待 barrier", quarter + 1, r + 1, sales);
                        barrier.await(5, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        try {
            done.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdownNow();

        Map<String, Object> data = new HashMap<>();
        data.put("yearTotal", yearTotal.get());
        data.put("rounds", rounds);
        return ScenarioResult.of(log, String.format("CyclicBarrier 跑了 %d 轮，年报汇总=%d万，证明它可复用", rounds, yearTotal.get()), data);
    }

    /**
     * Semaphore 接口限流。
     *
     * 八股：连接池/限流器本质就是 Semaphore；tryAcquire(超时) 拿不到可降级；fair 参数控制公平性。
     */
    public ScenarioResult semaphoreLimit(int requests, int permits) {
        ScenarioLog log = new ScenarioLog();
        Semaphore semaphore = new Semaphore(permits);
        AtomicInteger peak = new AtomicInteger();
        AtomicInteger current = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requests);
        ExecutorService pool = Executors.newFixedThreadPool(requests, new NamedThreadFactory("semaphore-"));

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < requests; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    semaphore.acquire();
                    int now = current.incrementAndGet();
                    int max;
                    do {
                        max = peak.get();
                    } while (now > max && !peak.compareAndSet(max, now));
                    log.log("request-%d 拿到许可，当前并发=%d", idx, now);
                    Thread.sleep(500);
                    current.decrementAndGet();
                    log.log("request-%d 释放许可", idx);
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        try {
            done.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long elapsed = System.currentTimeMillis() - t0;
        pool.shutdownNow();

        // 快速失败演示：许可=1，5 请求竞争 tryAcquire(200ms)
        Semaphore strict = new Semaphore(1);
        AtomicInteger degraded = new AtomicInteger();
        CountDownLatch start2 = new CountDownLatch(1);
        CountDownLatch done2 = new CountDownLatch(5);
        ExecutorService pool2 = Executors.newFixedThreadPool(5, new NamedThreadFactory("semaphore-degrade-"));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            pool2.submit(() -> {
                try {
                    start2.await();
                    if (strict.tryAcquire(200, TimeUnit.MILLISECONDS)) {
                        try {
                            Thread.sleep(300);
                            log.log("degrade-request-%d 拿到许可", idx);
                        } finally {
                            strict.release();
                        }
                    } else {
                        degraded.incrementAndGet();
                        log.log("degrade-request-%d 200ms 未拿到许可，快速失败", idx);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done2.countDown();
                }
            });
        }
        start2.countDown();
        try {
            done2.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool2.shutdownNow();

        Map<String, Object> data = new HashMap<>();
        data.put("permits", permits);
        data.put("requests", requests);
        data.put("peakConcurrency", peak.get());
        data.put("totalSent", requests);
        data.put("elapsedMs", elapsed);
        data.put("degradedCount", degraded.get());
        String summary = String.format("峰值并发=%d（<= 许可 %d），降级请求=%d；Semaphore 是限流/连接池的本质",
                peak.get(), permits, degraded.get());
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * Exchanger 双人对账。
     *
     * 八股：必须成双成对；带超时的 exchange 避免单方永久阻塞。
     */
    public ScenarioResult exchangerReconcile() {
        ScenarioLog log = new ScenarioLog();
        Exchanger<Integer> exchanger = new Exchanger<>();

        int[] aBills = {100, 200, 150, 120, 180};
        int[] bBills = {90, 210, 130, 170, 140};
        int aSum = Arrays.stream(aBills).sum();
        int bSum = Arrays.stream(bBills).sum();
        AtomicInteger match = new AtomicInteger();

        Thread tA = new Thread(() -> {
            try {
                log.log("A 核对 1~5 号账单，小计=%d", aSum);
                Integer other = exchanger.exchange(aSum);
                log.log("A 拿到 B 的小计=%d，总账=%d，校验 %s", other, aSum + other,
                        (aSum + other == aSum + bSum ? "一致" : "不一致"));
                if (aSum + other == aSum + bSum) match.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "exchanger-A");
        tA.setDaemon(true);

        Thread tB = new Thread(() -> {
            try {
                log.log("B 核对 6~10 号账单，小计=%d", bSum);
                Integer other = exchanger.exchange(bSum);
                log.log("B 拿到 A 的小计=%d，总账=%d，校验 %s", other, aSum + bSum,
                        (aSum + other == aSum + bSum ? "一致" : "不一致"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "exchanger-B");
        tB.setDaemon(true);

        tA.start();
        tB.start();
        try {
            tA.join(3000);
            tB.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 单边超时演示
        Exchanger<Integer> lone = new Exchanger<>();
        AtomicInteger timeoutCaught = new AtomicInteger();
        Thread tC = new Thread(() -> {
            try {
                log.log("单边线程调用 exchange(1s)，无人配对…");
                lone.exchange(123, 1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                timeoutCaught.incrementAndGet();
                log.log("单边 exchange 1s 后超时，捕获 TimeoutException");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "exchanger-lone");
        tC.setDaemon(true);
        tC.start();
        try {
            tC.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("ledgerMatch", match.get() > 0);
        data.put("timeoutCaught", timeoutCaught.get() > 0);
        return ScenarioResult.of(log, "Exchanger 双线程互换数据完成对账；单方调用会超时", data);
    }
}

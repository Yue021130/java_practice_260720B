package com.example.juc.atomic;

import com.example.juc.common.NamedThreadFactory;
import com.example.juc.common.ScenarioLog;
import com.example.juc.common.ScenarioResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * 原子类场景业务实现。
 *
 * 面试点：CAS 是 CPU cmpxchg 指令；AtomicLong 高并发下竞争激烈；LongAdder 用 Cell 分段降低热点；
 * ABA 要用版本号解决；AtomicReference 适合做无锁状态机。
 */
@Slf4j
@Service
public class AtomicScenarioService {

    /**
     * 库存扣减三路对比：裸 int / synchronized / AtomicLong。
     *
     * 八股：i++ 不是原子的；synchronized 正确但串行；CAS 无锁正确且快。
     */
    public ScenarioResult stockCompare(int threads, int times) {
        ScenarioLog log = new ScenarioLog();
        int expected = threads * times;

        // 1) 裸 int：共享可变 int 字段，无同步
        IntHolder plain = new IntHolder(0);
        long plainElapsed = runRace(threads, times, () -> plain.value++, "plain", log);

        // 2) synchronized 方法
        SyncCounter syncCounter = new SyncCounter();
        long syncElapsed = runRace(threads, times, syncCounter::increment, "sync", log);

        // 3) AtomicLong
        AtomicLong atomic = new AtomicLong(0);
        long atomicElapsed = runRace(threads, times, atomic::incrementAndGet, "atomic", log);

        Map<String, Object> data = new HashMap<>();
        data.put("expected", expected);
        data.put("plainResult", plain.value);
        data.put("plainElapsedMs", plainElapsed);
        data.put("syncResult", syncCounter.get());
        data.put("syncElapsedMs", syncElapsed);
        data.put("atomicResult", atomic.get());
        data.put("atomicElapsedMs", atomicElapsed);
        data.put("lostUpdates", expected - plain.value);

        String summary = String.format("裸 int 结果 %d（丢失 %d 次），synchronized %d，AtomicLong %d；CAS 无锁最快",
                plain.value, expected - plain.value, syncCounter.get(), atomic.get());
        return ScenarioResult.of(log, summary, data);
    }

    private long runRace(int threads, int times, Runnable task, String tag, ScenarioLog log) {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads, new NamedThreadFactory(tag + "-"));
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < times; j++) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        try {
            done.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long elapsed = System.currentTimeMillis() - t0;
        pool.shutdownNow();
        log.log("%s 完成：耗时 %dms", tag, elapsed);
        return elapsed;
    }

    /**
     * LongAdder vs AtomicLong 高并发压测。
     *
     * 八股：LongAdder 用 base + Cell[] 分段，不同线程写不同 Cell；sum() 是弱一致快照。
     */
    public ScenarioResult longadderVsAtomic(int threads, int times) {
        ScenarioLog log = new ScenarioLog();
        int expected = threads * times;

        AtomicLong atomic = new AtomicLong(0);
        long atomicElapsed = runRace(threads, times, atomic::incrementAndGet, "atomic", log);

        LongAdder adder = new LongAdder();
        long adderElapsed = runRace(threads, times, adder::increment, "longadder", log);

        Map<String, Object> data = new HashMap<>();
        data.put("expected", expected);
        data.put("atomicResult", atomic.get());
        data.put("atomicElapsedMs", atomicElapsed);
        data.put("longAdderResult", adder.sum());
        data.put("longAdderElapsedMs", adderElapsed);
        double speedup = atomicElapsed > 0 ? (double) atomicElapsed / Math.max(1, adderElapsed) : 0;
        data.put("speedup", String.format("%.1fx", speedup));

        String summary = String.format("AtomicLong=%d(%dms) LongAdder=%d(%dms) 高并发下 LongAdder 通常快 %.1f 倍",
                atomic.get(), atomicElapsed, adder.sum(), adderElapsed, speedup);
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * ABA 问题与修复。
     *
     * 八股：AtomicReference 只看值，ABA 中间变化无法感知；AtomicStampedReference 加版本号可识破。
     */
    public ScenarioResult aba() {
        ScenarioLog log = new ScenarioLog();
        Map<String, Object> data = new HashMap<>();

        // 第一组：AtomicReference 复现 ABA
        AtomicReference<Integer> balance = new AtomicReference<>(100);
        CountDownLatch t1Done = new CountDownLatch(1);
        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(1200);
                log.log("t1 睡醒，准备 CAS(100→50)");
                boolean success = balance.compareAndSet(100, 50);
                log.log("t1 CAS 结果：%s，当前值=%d", success, balance.get());
                data.put("plainCasSuccess", success);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                t1Done.countDown();
            }
        }, "aba-t1");
        t1.setDaemon(true);

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(300);
                balance.compareAndSet(100, 50);
                log.log("t2 第一次扣款：100→50");
                Thread.sleep(100);
                balance.compareAndSet(50, 100);
                log.log("t2 第二次充值：50→100（ABA 完成，值回到 100）");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "aba-t2");
        t2.setDaemon(true);

        t1.start();
        t2.start();
        try {
            t2.join(3000);
            t1.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 第二组：AtomicStampedReference 用版本号识破
        AtomicStampedReference<Integer> stamped = new AtomicStampedReference<>(100, 0);
        CountDownLatch t3Done = new CountDownLatch(1);
        Thread t3 = new Thread(() -> {
            try {
                Thread.sleep(1200);
                int[] stampHolder = new int[1];
                int current = stamped.get(stampHolder);
                log.log("t3 睡醒，期望 stamp=%d 时 CAS(100→50)", stampHolder[0]);
                boolean success = stamped.compareAndSet(100, 50, stampHolder[0], stampHolder[0] + 1);
                log.log("t3 带版本号 CAS 结果：%s，当前 ref=%d stamp=%d", success, stamped.get(stampHolder), stampHolder[0]);
                data.put("stampedCasSuccess", success);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                t3Done.countDown();
            }
        }, "aba-t3");
        t3.setDaemon(true);

        Thread t4 = new Thread(() -> {
            try {
                Thread.sleep(300);
                int[] stampHolder = new int[1];
                int current = stamped.get(stampHolder);
                stamped.compareAndSet(100, 50, stampHolder[0], stampHolder[0] + 1);
                log.log("t4 第一次扣款：100→50，stamp 变为 1");
                Thread.sleep(100);
                stamped.get(stampHolder); // stamp 写入 holder[0]
                stamped.compareAndSet(50, 100, stampHolder[0], stampHolder[0] + 1);
                log.log("t4 第二次充值：50→100，stamp 变为 2（版本号已变）");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "aba-t4");
        t4.setDaemon(true);

        t3.start();
        t4.start();
        try {
            t4.join(3000);
            t3.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String summary = "AtomicReference 中 ABA 被 CAS 无视（成功=true），AtomicStampedReference 因版本号变化识破（成功=false）";
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * 无锁订单状态机。
     *
     * 八股：compareAndSet(旧状态, 新状态) 天然保证迁移合法性；数据库等价写法是 update where status=旧状态。
     */
    public ScenarioResult orderState() {
        ScenarioLog log = new ScenarioLog();
        AtomicReference<String> state = new AtomicReference<>("待支付");

        int payThreads = 20;
        int shipThreads = 20;
        int illegalThreads = 20;

        AtomicInteger paySuccess = new AtomicInteger();
        AtomicInteger shipSuccess = new AtomicInteger();
        AtomicInteger illegalRejected = new AtomicInteger();

        paySuccess.set(runCasRace(payThreads, () -> state.compareAndSet("待支付", "已支付"), "pay", log));
        shipSuccess.set(runCasRace(shipThreads, () -> state.compareAndSet("已支付", "已发货"), "ship", log));
        illegalRejected.set(runCasRace(illegalThreads, () -> state.compareAndSet("待支付", "已发货"), "illegal", log));

        Map<String, Object> data = new HashMap<>();
        data.put("paySuccessCount", paySuccess.get());
        data.put("shipSuccessCount", shipSuccess.get());
        data.put("illegalRejected", illegalRejected.get());
        data.put("finalState", state.get());

        String summary = String.format("支付回调 %d 个仅 1 个成功，发货 %d 个仅 1 个成功，非法迁移 %d 个全部失败；CAS 状态机防重放",
                payThreads, shipThreads, illegalThreads);
        return ScenarioResult.of(log, summary, data);
    }

    private int runCasRace(int threads, java.util.function.BooleanSupplier casTask, String tag, ScenarioLog log) {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads, new NamedThreadFactory(tag + "-"));
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (casTask.getAsBoolean()) {
                        success.incrementAndGet();
                        log.log("%s 线程 CAS 成功", Thread.currentThread().getName());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        try {
            done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdownNow();
        return success.get();
    }

    // ---------- 内部辅助类 ----------

    private static class IntHolder {
        int value;
        IntHolder(int initial) { this.value = initial; }
    }

    private static class SyncCounter {
        private int value;
        synchronized void increment() { value++; }
        synchronized int get() { return value; }
    }
}

package com.example.juc.base;

import com.example.juc.common.NamedThreadFactory;
import com.example.juc.common.ScenarioLog;
import com.example.juc.common.ScenarioResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JMM 基础场景业务实现。
 *
 * 面试点：volatile 可见性 + 有序性但不保证原子性；DCL 必须 volatile 防指令重排；
 * 死锁排查用 jps/jstack/ThreadMXBean。
 */
@Slf4j
@Service
public class BaseScenarioService {

    /**
     * volatile 可见性。
     *
     * 八股：JMM 主存/工作内存；volatile 写 happens-before 后续读；禁止指令重排。
     */
    public ScenarioResult volatileVisibility() {
        ScenarioLog log = new ScenarioLog();
        Map<String, Object> data = new HashMap<>();

        // 实验一：普通 boolean（JIT 优化后可能把 stop 提到寄存器常量，导致永远看不到）
        PlainStopHolder plain = new PlainStopHolder();
        AtomicInteger plainCounter = new AtomicInteger();
        Thread plainWorker = new Thread(() -> {
            long c = 0;
            while (!plain.stop) {
                c++;
            }
            plainCounter.set((int) Math.min(c, Integer.MAX_VALUE));
            log.log("plain-worker 看到 stop=true，循环了 %d 次", c);
        }, "plain-worker");
        plainWorker.setDaemon(true);
        plainWorker.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        plain.stop = true;
        log.log("main 设置 plain.stop=true");
        try {
            plainWorker.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean plainStopped = !plainWorker.isAlive();
        plainWorker.interrupt();
        data.put("plainStopped", plainStopped);
        data.put("plainCounter", plainCounter.get());
        if (!plainStopped) {
            log.log("plain-worker 2 秒内未退出：普通变量不可见（概率性，取决于 JIT 优化）");
        }

        // 实验二：volatile boolean
        VolatileStopHolder vol = new VolatileStopHolder();
        AtomicInteger volCounter = new AtomicInteger();
        Thread volWorker = new Thread(() -> {
            long c = 0;
            while (!vol.stop) {
                c++;
            }
            volCounter.set((int) Math.min(c, Integer.MAX_VALUE));
            log.log("volatile-worker 看到 stop=true，循环了 %d 次", c);
        }, "volatile-worker");
        volWorker.setDaemon(true);
        volWorker.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        vol.stop = true;
        log.log("main 设置 volatile.stop=true");
        try {
            volWorker.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean volatileStopped = !volWorker.isAlive();
        volWorker.interrupt();
        data.put("volatileStopped", volatileStopped);
        data.put("volatileCounter", volCounter.get());

        String summary = String.format("普通变量 stopped=%s（概率性不可见），volatile stopped=%s（立即可见）",
                plainStopped, volatileStopped);
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * volatile 不保证原子性。
     *
     * 八股：i++ 读-改-写三步非原子；volatile 只保证可见/有序。
     */
    public ScenarioResult volatileNotAtomic(int threads, int times) {
        ScenarioLog log = new ScenarioLog();
        VolatileCounter vc = new VolatileCounter();
        AtomicInteger ai = new AtomicInteger();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads * 2);
        ExecutorService pool = Executors.newFixedThreadPool(threads * 2, new NamedThreadFactory("vol-atomic-"));

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < times; j++) vc.increment();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < times; j++) ai.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        try {
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdownNow();

        int expected = threads * times;
        Map<String, Object> data = new HashMap<>();
        data.put("expected", expected);
        data.put("volatileResult", vc.get());
        data.put("lostUpdates", expected - vc.get());
        data.put("atomicResult", ai.get());
        String summary = String.format("volatile 结果 %d， AtomicInteger %d，丢失约 %d 次更新",
                vc.get(), ai.get(), expected - vc.get());
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * DCL 双重检查单例。
     *
     * 八股：instance = new 非原子（分配→初始化→赋值可能重排），volatile 禁止重排；
     * 静态内部类 Holder 利用类加载机制保证线程安全。
     */
    public ScenarioResult dclSingleton() {
        ScenarioLog log = new ScenarioLog();
        Set<Integer> dclHashes = new HashSet<>();
        Set<Integer> holderHashes = new HashSet<>();
        int threads = 100;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads, new NamedThreadFactory("dcl-"));

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    DclSingleton s = DclSingleton.getInstance();
                    DclHolderSingleton h = DclHolderSingleton.getInstance();
                    synchronized (dclHashes) {
                        dclHashes.add(System.identityHashCode(s));
                        holderHashes.add(System.identityHashCode(h));
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

        log.log("DCL volatile 写法：%d 个线程拿到 %d 个不同实例", threads, dclHashes.size());
        log.log("静态内部类 Holder 写法：%d 个线程拿到 %d 个不同实例", threads, holderHashes.size());

        Map<String, Object> data = new HashMap<>();
        data.put("distinctInstances", dclHashes.size());
        data.put("holderDistinctInstances", holderHashes.size());
        return ScenarioResult.of(log, "DCL 必须加 volatile，静态内部类 Holder 也是线程安全写法", data);
    }

    /**
     * 死锁制造与自动检测。
     *
     * 八股：排查三板斧 jps/jstack/ThreadMXBean；关注 BLOCKED 状态和 "Found one Java-level deadlock"。
     */
    public ScenarioResult deadlockDetect() {
        ScenarioLog log = new ScenarioLog();
        ReentrantLock lockA = new ReentrantLock();
        ReentrantLock lockB = new ReentrantLock();

        Thread t1 = new Thread(() -> {
            try {
                lockA.lockInterruptibly();
                log.log("deadlock-t1 持有 lockA，准备拿 lockB");
                Thread.sleep(500);
                lockB.lockInterruptibly();
                try {
                    log.log("deadlock-t1 拿到 lockB");
                } finally {
                    lockB.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.log("deadlock-t1 被中断，释放等待");
            } finally {
                if (lockA.isHeldByCurrentThread()) lockA.unlock();
            }
        }, "deadlock-t1");
        t1.setDaemon(true);

        Thread t2 = new Thread(() -> {
            try {
                lockB.lockInterruptibly();
                log.log("deadlock-t2 持有 lockB，准备拿 lockA");
                Thread.sleep(500);
                lockA.lockInterruptibly();
                try {
                    log.log("deadlock-t2 拿到 lockA");
                } finally {
                    lockA.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.log("deadlock-t2 被中断，释放等待");
            } finally {
                if (lockB.isHeldByCurrentThread()) lockB.unlock();
            }
        }, "deadlock-t2");
        t2.setDaemon(true);

        t1.start();
        t2.start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = bean.findDeadlockedThreads();
        boolean detected = ids != null && ids.length > 0;
        StringBuilder names = new StringBuilder();
        if (detected) {
            ThreadInfo[] infos = bean.getThreadInfo(ids);
            for (ThreadInfo info : infos) {
                names.append(info.getThreadName()).append(" ");
                log.log("检测到死锁线程：%s，等待锁：%s", info.getThreadName(),
                        info.getLockName() == null ? "unknown" : info.getLockName());
            }
        } else {
            log.log("未检测到死锁（可能线程尚未进入死锁态）");
        }

        t1.interrupt();
        t2.interrupt();
        try {
            t1.join(2000);
            t2.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deadlockDetected", detected);
        data.put("deadlockedThreads", names.toString().trim());
        data.put("resolved", !t1.isAlive() && !t2.isAlive());
        return ScenarioResult.of(log, detected ? "检测到死锁线程，已中断解开" : "未稳定复现死锁，代码中演示了检测逻辑", data);
    }

    // ---------- 内部辅助类 ----------

    private static class PlainStopHolder {
        boolean stop;
    }

    private static class VolatileStopHolder {
        volatile boolean stop;
    }

    private static class VolatileCounter {
        volatile int value;
        void increment() { value++; }
        int get() { return value; }
    }

    private static class DclSingleton {
        private static volatile DclSingleton instance;
        private DclSingleton() {}
        static DclSingleton getInstance() {
            if (instance == null) {
                synchronized (DclSingleton.class) {
                    if (instance == null) {
                        instance = new DclSingleton();
                    }
                }
            }
            return instance;
        }
    }

    private static class DclHolderSingleton {
        private DclHolderSingleton() {}
        private static class Holder {
            private static final DclHolderSingleton INSTANCE = new DclHolderSingleton();
        }
        static DclHolderSingleton getInstance() {
            return Holder.INSTANCE;
        }
    }
}

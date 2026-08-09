package com.example.unsafe.cas;

import com.example.unsafe.common.UnsafeBizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 04. CAS 原子操作：compareAndSwapInt 自旋计数器、三种自增性能对比、ABA 问题现场复现。
 *
 * <p>CAS = Compare And Swap，硬件级原子指令（x86 的 CMPXCHG + lock 前缀）。
 * JUC 的 AtomicInteger / ConcurrentHashMap / AQS 全部构建在 Unsafe 的 CAS 之上。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CasService {

    private final Unsafe unsafe;

    /** Unsafe CAS 自旋计数器的目标对象：字段偏移在构造时算好 */
    private static final CasTarget TARGET = new CasTarget();
    private static final long VALUE_OFFSET;

    static {
        try {
            VALUE_OFFSET = UnsafeHolder.unsafe().objectFieldOffset(CasTarget.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** 让静态块能拿到 Unsafe（Spring 注入时机晚于类加载，这里用全局实例） */
    private static class UnsafeHolder {
        static Unsafe unsafe() {
            try {
                java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return (sun.misc.Unsafe) f.get(null);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    public static class CasTarget {
        volatile int value;
    }

    /**
     * 单线程自旋 CAS 计数器：直到 CAS 成功才退出，统计尝试次数。
     */
    public Map<String, Object> spin(int times) {
        if (times <= 0 || times > 10000000) {
            throw new UnsafeBizException("times 需在 1 ~ 10000000 之间");
        }
        TARGET.value = 0;
        long attempts = 0;
        for (int i = 0; i < times; i++) {
            while (true) {
                int expect = unsafe.getIntVolatile(TARGET, VALUE_OFFSET);
                attempts++;
                if (unsafe.compareAndSwapInt(TARGET, VALUE_OFFSET, expect, expect + 1)) {
                    break;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("times", times);
        result.put("finalValue", TARGET.value);
        result.put("totalCasAttempts", attempts);
        result.put("tip", "CAS 失败就重读重试（自旋）。单线程下每次都一次成功，所以 attempts = times；"
                + "多线程竞争激烈时 attempts 会明显大于 times。");
        return result;
    }

    /**
     * 三种计数器并发自增性能对比：synchronized / AtomicInteger / Unsafe CAS 自旋。
     */
    public Map<String, Object> benchmark(int threads, int times) {
        if (threads <= 0 || threads > 16) {
            throw new UnsafeBizException("threads 需在 1 ~ 16 之间");
        }
        if (times <= 0 || times > 5000000) {
            throw new UnsafeBizException("times 需在 1 ~ 5000000 之间");
        }

        long syncMs = timeSync(threads, times);
        long atomicMs = timeAtomic(threads, times);
        long casMs = timeCas(threads, times);
        long expected = (long) threads * times;

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("synchronized", syncMs, "JVM 管锁：偏向锁→轻量锁→重量锁，冲突时挂起线程"));
        rows.add(row("AtomicInteger", atomicMs, "Unsafe CAS 自旋：乐观重试，不挂起线程"));
        rows.add(row("Unsafe CAS 自旋", casMs, "手写 getIntVolatile + compareAndSwapInt，原子类底层一模一样"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("threads", threads);
        result.put("timesPerThread", times);
        result.put("expectedTotal", expected);
        result.put("rows", rows);
        result.put("tip", "耗时单位 ms，数值随机器/系统负载波动，重点看相对关系：CAS 竞争激烈时自旋空转可能反而更慢，"
                + "这也是 LongAdder 用“分段计数”的原因。");
        return result;
    }

    /**
     * ABA 问题现场复现：线程 A 读 100 → 线程 B 改 100→200→100 → A 的 CAS(100→50) 结果如何？
     */
    public Map<String, Object> aba() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scenario", "账户余额初始 100。线程 A 读到余额 100 后暂停；线程 B 先存 100（→200）再取 100（→100）；"
                + "随后线程 A 想扣款 50。余额数值回到 100，A 的扣款该不该成功？");
        result.put("naive", runAba(false));
        result.put("versioned", runAba(true));
        result.put("conclusion", "只比数值：余额“看起来没变”，A 的 CAS 成功——但它对余额的“中间经历”一无所知，这就是 ABA。"
                + "带上版本号（或 AtomicStampedReference）：版本从 0→1→2 变了，A 的 CAS 失败，ABA 被识别。");
        return result;
    }

    /**
     * CAS 原理八股速记。
     */
    public Map<String, Object> explain() {
        List<Map<String, String>> items = new ArrayList<>();
        items.add(map("是什么", "Compare And Swap：一条硬件原子指令，比较内存值是否等于期望值，等于才写新值，整体不可打断"));
        items.add(map("与锁的区别", "锁是“悲观”：先锁再干活，冲突就挂起；CAS 是“乐观”：直接尝试，失败就重试（自旋）"));
        items.add(map("Java 里的落地", "sun.misc.Unsafe 的 compareAndSwap* → 内部是 native，最终落到 CPU 指令"));
        items.add(map("JUC 谁在用", "AtomicInteger/Long、ConcurrentHashMap、AQS（ReentrantLock/Semaphore 的底层）、ThreadPoolExecutor 的 ctl"));
        items.add(map("三大问题", "① 自旋空转耗 CPU；② 只能保证一个变量的原子性；③ ABA 问题——用版本号 / AtomicStampedReference 解决"));
        items.add(map("参数含义", "compareAndSwapInt(对象, 字段偏移, 期望值, 新值)：偏移决定对哪个字段动手"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("tip", "面试必问：CAS 与 synchronized 的区别？CAS 的三大问题？ABA 如何解决？");
        return result;
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /** 复现一次 ABA（useVersion=true 时带版本号），返回该轮结局 */
    private Map<String, Object> runAba(boolean useVersion) {
        Account acc = new Account();
        acc.balance = 100;
        acc.version = 0;
        long balanceOffset = unsafe.objectFieldOffset(fieldOf(Account.class, "balance"));
        long versionOffset = unsafe.objectFieldOffset(fieldOf(Account.class, "version"));

        CountDownLatch aReadDone = new CountDownLatch(1);
        CountDownLatch aGo = new CountDownLatch(1);
        AtomicBoolean aSuccess = new AtomicBoolean(false);

        // 线程 A：读到余额（和版本）后暂停，等 B 折腾完再 CAS 扣款
        Thread a = new Thread(() -> {
            int expectBalance = unsafe.getIntVolatile(acc, balanceOffset);
            int expectVersion = unsafe.getIntVolatile(acc, versionOffset);
            aReadDone.countDown();
            try {
                aGo.await();
            } catch (InterruptedException e) {
                return;
            }
            boolean ok;
            if (useVersion) {
                // 带版本号：余额 + 版本一起 CAS，版本对不上就失败
                ok = unsafe.compareAndSwapInt(acc, balanceOffset, expectBalance, 50)
                        && unsafe.compareAndSwapInt(acc, versionOffset, expectVersion, expectVersion + 1);
            } else {
                // 只比数值：余额回到 100 就认为“没动过”
                ok = unsafe.compareAndSwapInt(acc, balanceOffset, expectBalance, 50);
            }
            aSuccess.set(ok);
        }, "A-ABA-" + (useVersion ? "versioned" : "naive"));

        // 线程 B：A 读完后再存再取（100→200→100）
        Thread b = new Thread(() -> {
            try {
                aReadDone.await();
            } catch (InterruptedException e) {
                return;
            }
            spinCas(acc, balanceOffset, 100, 200); // 存 100
            if (useVersion) {
                spinCas(acc, versionOffset, 0, 1);
            }
            spinCas(acc, balanceOffset, 200, 100); // 取 100
            if (useVersion) {
                spinCas(acc, versionOffset, 1, 2);
            }
            aGo.countDown();
        }, "B-ABA-" + (useVersion ? "versioned" : "naive"));

        a.start();
        b.start();
        try {
            a.join(5000);
            b.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("useVersion", useVersion);
        result.put("balanceAfter", acc.balance);
        result.put("versionAfter", acc.version);
        result.put("aCasSucceeded", aSuccess.get());
        result.put("meaning", useVersion
                ? "版本号 0→1→2 已变化，A 的 CAS 失败 → ABA 被识别，扣款被拒绝（安全）"
                : "余额数值回到 100，A 的 CAS 成功 → ABA 未识别，扣款被批准（有隐患）");
        return result;
    }

    /** 自旋 CAS 直到成功 */
    private void spinCas(Account acc, long offset, int expect, int update) {
        while (!unsafe.compareAndSwapInt(acc, offset, expect, update)) {
            Thread.yield();
        }
    }

    private static java.lang.reflect.Field fieldOf(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    /** CAS 目标：余额 + 版本号 */
    public static class Account {
        volatile int balance;
        volatile int version;
    }

    private long timeSync(int threads, int times) {
        IntCounter counter = new IntCounter();
        return measure(threads, times, () -> counter.syncInc());
    }

    private long timeAtomic(int threads, int times) {
        AtomicInteger counter = new AtomicInteger();
        return measure(threads, times, counter::incrementAndGet);
    }

    private long timeCas(int threads, int times) {
        CasTarget target = new CasTarget();
        return measure(threads, times, () -> {
            while (true) {
                int expect = unsafe.getIntVolatile(target, VALUE_OFFSET);
                if (unsafe.compareAndSwapInt(target, VALUE_OFFSET, expect, expect + 1)) {
                    return;
                }
            }
        });
    }

    /** 用 N 个线程各跑 times 次给定操作，返回总耗时 ms */
    private long measure(int threads, int times, Runnable op) {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int j = 0; j < times; j++) {
                    op.run();
                }
                done.countDown();
            });
            t.start();
        }
        try {
            ready.await();
            long begin = System.nanoTime();
            start.countDown();
            done.await();
            return (System.nanoTime() - begin) / 1_000_000;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /** 同步计数：同一把锁 */
    public static class IntCounter {
        private int value;

        public synchronized void syncInc() {
            value++;
        }
    }

    private Map<String, Object> row(String name, long ms, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("ms", ms);
        m.put("note", note);
        return m;
    }

    private Map<String, String> map(String k, String v) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("item", k);
        m.put("detail", v);
        return m;
    }
}

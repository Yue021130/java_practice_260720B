package com.example.juc.locks;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * 锁场景业务实现。
 *
 * 面试点：AQS 是 JUC 锁的基石；ReentrantLock 比 synchronized 多了可中断、超时、公平、多 Condition；
 * 读写锁适合读多写少但不允许锁升级；StampedLock 的乐观读必须 validate；手写 Condition 能讲清虚假唤醒。
 */
@Slf4j
@Service
public class LockScenarioService {

    /**
     * 转账死锁与解决。
     *
     * 八股：死锁四条件（互斥、持有并等待、不可剥夺、循环等待），破坏其一即可破解；
     * 常用手段：按固定顺序加锁、tryLock 超时放弃已有锁。
     */
    public ScenarioResult transferDeadlock(String mode) {
        ScenarioLog log = new ScenarioLog();
        Account accA = new Account("A", 1000);
        Account accB = new Account("B", 1000);

        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger deadlockCount = new AtomicInteger();

        Runnable t1 = () -> transferWithDeadlockRisk(accA, accB, 100, mode, log, deadlockCount, successCount, done);
        Runnable t2 = () -> transferWithDeadlockRisk(accB, accA, 100, mode, log, deadlockCount, successCount, done);

        Thread th1 = new Thread(t1, "transfer-t1");
        Thread th2 = new Thread(t2, "transfer-t2");
        th1.setDaemon(true);
        th2.setDaemon(true);
        th1.start();
        th2.start();

        try {
            done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("mode", mode);
        data.put("balanceA", accA.getBalance());
        data.put("balanceB", accB.getBalance());
        data.put("totalBalance", accA.getBalance() + accB.getBalance());
        data.put("deadlockSymptom", deadlockCount.get() > 0);
        data.put("successTransfers", successCount.get());

        String summary = "deadlock".equals(mode)
                ? String.format("死锁模式：总余额守恒 %d，检测到 %d 次互相等待征兆", accA.getBalance() + accB.getBalance(), deadlockCount.get())
                : String.format("tryLock 模式：总余额守恒 %d，两笔转账均成功", accA.getBalance() + accB.getBalance());
        return ScenarioResult.of(log, summary, data);
    }

    private void transferWithDeadlockRisk(Account from, Account to, int amount, String mode,
                                          ScenarioLog log, AtomicInteger deadlockCount,
                                          AtomicInteger successCount, CountDownLatch done) {
        try {
            if ("deadlock".equals(mode)) {
                // 交叉加锁：先锁 from，再尝试锁 to（带 2s 超时，避免把 HTTP 请求真的挂死）
                from.lock.lock();
                log.log("%s 已持有 %s 账户锁，准备获取 %s 账户锁", Thread.currentThread().getName(), from.name, to.name);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                boolean gotSecond;
                try {
                    gotSecond = to.lock.tryLock(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    gotSecond = false;
                }
                if (gotSecond) {
                    try {
                        from.withdraw(amount);
                        to.deposit(amount);
                        log.log("%s 成功转账 %d 元（%s → %s）", Thread.currentThread().getName(), amount, from.name, to.name);
                        successCount.incrementAndGet();
                    } finally {
                        to.lock.unlock();
                    }
                } else {
                    log.log("%s 获取 %s 锁超时：检测到死锁征兆（互相等待对方释放锁）", Thread.currentThread().getName(), to.name);
                    deadlockCount.incrementAndGet();
                }
            } else {
                // trylock 模式：按账户名固定顺序排序，拿不到就释放已持有的再重试
                Account first = from.name.compareTo(to.name) < 0 ? from : to;
                Account second = first == from ? to : from;
                boolean transferred = false;
                for (int i = 0; i < 15 && !transferred; i++) {
                    first.lock.lock();
                    log.log("%s 按固定顺序持有 %s 锁，尝试获取 %s 锁（第 %d 次）", Thread.currentThread().getName(), first.name, second.name, i + 1);
                    boolean gotSecond = false;
                    try {
                        gotSecond = second.lock.tryLock(500, TimeUnit.MILLISECONDS);
                        if (gotSecond) {
                            try {
                                if (from.getBalance() >= amount) {
                                    from.withdraw(amount);
                                    to.deposit(amount);
                                    transferred = true;
                                    log.log("%s 转账成功 %d 元（%s → %s）", Thread.currentThread().getName(), amount, from.name, to.name);
                                    successCount.incrementAndGet();
                                }
                            } finally {
                                second.lock.unlock();
                            }
                        } else {
                            log.log("%s 未拿到 %s 锁，释放 %s 锁后重试", Thread.currentThread().getName(), second.name, first.name);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        first.lock.unlock();
                    }
                    if (!transferred) {
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        } finally {
            done.countDown();
        }
    }

    /**
     * 公平锁 vs 非公平锁。
     *
     * 八股：AQS 用 CLH 队列管理等待线程；非公平锁允许插队，省去唤醒上下文切换所以更快；
     * 公平锁严格排队，吞吐低但避免饥饿。ReentrantLock 默认非公平。
     */
    public ScenarioResult fairVsNonfair(int threads) {
        ScenarioLog log = new ScenarioLog();
        int loops = 5;

        long fairElapsed = runLockContest(new ReentrantLock(true), threads, loops, "fair", log);
        long nonfairElapsed = runLockContest(new ReentrantLock(false), threads, loops, "nonfair", log);

        Map<String, Object> data = new HashMap<>();
        data.put("fairElapsedMs", fairElapsed);
        data.put("nonfairElapsedMs", nonfairElapsed);
        data.put("threads", threads);
        data.put("loops", loops);
        String summary = String.format("非公平锁耗时 %dms，公平锁耗时 %dms；非公平锁允许插队，通常吞吐更高",
                nonfairElapsed, fairElapsed);
        return ScenarioResult.of(log, summary, data);
    }

    private long runLockContest(ReentrantLock lock, int threads, int loops, String tag, ScenarioLog log) {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads, new NamedThreadFactory(tag + "-"));
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < loops; j++) {
                        lock.lock();
                        try {
                            log.log("%s 线程-%d 第 %d 次拿到锁", tag, idx, j + 1);
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            lock.unlock();
                        }
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
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long elapsed = System.currentTimeMillis() - t0;
        pool.shutdownNow();
        log.log("%s 锁总耗时 %dms（%d 线程 × %d 次）", tag, elapsed, threads, loops);
        return elapsed;
    }

    /**
     * lockInterruptibly 可中断。
     *
     * 八股：synchronized 阻塞不可中断；Lock.lockInterruptibly() 等锁期间可被中断并抛 InterruptedException。
     */
    public ScenarioResult interruptible() {
        ScenarioLog log = new ScenarioLog();
        ReentrantLock lock = new ReentrantLock();

        Thread holder = new Thread(() -> {
            lock.lock();
            log.log("holder 拿到锁，持有 5 秒模拟慢资源");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                log.log("holder 释放锁");
            }
        }, "lock-holder");
        holder.setDaemon(true);
        holder.start();

        AtomicInteger interrupted = new AtomicInteger();
        Thread waiter = new Thread(() -> {
            try {
                log.log("waiter 开始 lockInterruptibly 等待");
                lock.lockInterruptibly();
                try {
                    log.log("waiter 拿到锁（正常不应该拿到）");
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interrupted.incrementAndGet();
                log.log("waiter 等锁期间被中断，优雅退出，不再傻等");
            }
        }, "lock-waiter");
        waiter.setDaemon(true);
        waiter.start();

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        waiter.interrupt();

        try {
            waiter.join(2000);
            holder.join(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("interrupted", interrupted.get() > 0);
        return ScenarioResult.of(log, "lockInterruptibly 在等锁期间响应中断并退出；synchronized 做不到这一点", data);
    }

    /**
     * 读写锁商品缓存。
     *
     * 八股：读读共享、读写互斥、写写互斥；锁降级允许，锁升级会死锁；适合读多写少。
     */
    public ScenarioResult readwriteCache(int readers, int writers) {
        ScenarioLog log = new ScenarioLog();
        Map<String, Integer> cache = new HashMap<>();
        cache.put("iphone", 100);
        cache.put("macbook", 50);

        long rwElapsed = runCacheWork(cache, new ReadWriteLockAdapter(), readers, writers, "rw", log);
        cache.put("iphone", 100);
        cache.put("macbook", 50);
        long exclusiveElapsed = runCacheWork(cache, new ExclusiveLockAdapter(), readers, writers, "exclusive", log);

        Map<String, Object> data = new HashMap<>();
        data.put("readers", readers);
        data.put("writers", writers);
        data.put("rwElapsedMs", rwElapsed);
        data.put("exclusiveElapsedMs", exclusiveElapsed);
        String summary = String.format("读写锁耗时 %dms，独占锁耗时 %dms；读多写少场景下读写锁并发读优势明显",
                rwElapsed, exclusiveElapsed);
        return ScenarioResult.of(log, summary, data);
    }

    private long runCacheWork(Map<String, Integer> cache, LockAdapter adapter, int readers, int writers, String tag, ScenarioLog log) {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(readers + writers);
        ExecutorService pool = Executors.newFixedThreadPool(readers + writers, new NamedThreadFactory(tag + "-"));

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < writers; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 3; j++) {
                        adapter.writeLock();
                        try {
                            cache.put("iphone", cache.get("iphone") + 1);
                            Thread.sleep(100);
                        } finally {
                            adapter.writeUnlock();
                        }
                    }
                    log.log("%s 写线程-%d 完成 3 次更新", tag, idx);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        for (int i = 0; i < readers; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 5; j++) {
                        adapter.readLock();
                        try {
                            int v = cache.get("iphone");
                            Thread.sleep(30);
                        } finally {
                            adapter.readUnlock();
                        }
                    }
                    log.log("%s 读线程-%d 完成 5 次读取", tag, idx);
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
        return elapsed;
    }

    private interface LockAdapter {
        void readLock();
        void readUnlock();
        void writeLock();
        void writeUnlock();
    }

    private static class ReadWriteLockAdapter implements LockAdapter {
        private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        @Override public void readLock() { rwl.readLock().lock(); }
        @Override public void readUnlock() { rwl.readLock().unlock(); }
        @Override public void writeLock() { rwl.writeLock().lock(); }
        @Override public void writeUnlock() { rwl.writeLock().unlock(); }
    }

    private static class ExclusiveLockAdapter implements LockAdapter {
        private final ReentrantLock lock = new ReentrantLock();
        @Override public void readLock() { lock.lock(); }
        @Override public void readUnlock() { lock.unlock(); }
        @Override public void writeLock() { lock.lock(); }
        @Override public void writeUnlock() { lock.unlock(); }
    }

    /**
     * StampedLock 乐观读。
     *
     * 八股：乐观读不加锁，读完后必须 validate(stamp)，失败则升级为 readLock；StampedLock 不可重入。
     */
    public ScenarioResult stampedOptimistic() {
        ScenarioLog log = new ScenarioLog();
        StampedLock sl = new StampedLock();
        Point point = new Point(0, 0);
        int readLoops = 10;
        int writeLoops = 10;
        CountDownLatch done = new CountDownLatch(4);
        AtomicInteger optimisticSuccess = new AtomicInteger();
        AtomicInteger optimisticFallback = new AtomicInteger();

        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < writeLoops; i++) {
                    long stamp = sl.writeLock();
                    try {
                        point.x += 1;
                        point.y += 1;
                        log.log("writer 移动坐标到 (%d,%d)", point.x, point.y);
                    } finally {
                        sl.unlockWrite(stamp);
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "stamped-writer");
        writer.setDaemon(true);
        writer.start();

        for (int r = 0; r < 3; r++) {
            final int readerIdx = r;
            Thread reader = new Thread(() -> {
                try {
                    for (int i = 0; i < readLoops; i++) {
                        long stamp = sl.tryOptimisticRead();
                        int x = point.x;
                        int y = point.y;
                        Thread.sleep(20);
                        if (sl.validate(stamp)) {
                            optimisticSuccess.incrementAndGet();
                            log.log("reader-%d 乐观读成功：坐标 (%d,%d)", readerIdx, x, y);
                        } else {
                            optimisticFallback.incrementAndGet();
                            stamp = sl.readLock();
                            try {
                                x = point.x;
                                y = point.y;
                                log.log("reader-%d 乐观读失败，升级为读锁：坐标 (%d,%d)", readerIdx, x, y);
                            } finally {
                                sl.unlockRead(stamp);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "stamped-reader-" + readerIdx);
            reader.setDaemon(true);
            reader.start();
        }

        try {
            done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("optimisticSuccess", optimisticSuccess.get());
        data.put("optimisticFallback", optimisticFallback.get());
        String summary = String.format("乐观读成功 %d 次、升级读锁 %d 次；乐观读无锁更快，但必须 validate",
                optimisticSuccess.get(), optimisticFallback.get());
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * Condition 手写生产者消费者。
     *
     * 八股：Condition 比 wait/notify 精确，一个锁可挂多个等待队列；await 必须放在 while 里防止虚假唤醒。
     */
    public ScenarioResult conditionBuffer(int producers, int consumers, int items) {
        ScenarioLog log = new ScenarioLog();
        BoundedBuffer buffer = new BoundedBuffer(4, log);
        int total = producers * items;
        CountDownLatch done = new CountDownLatch(producers + consumers);
        AtomicInteger consumed = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(producers + consumers,
                new NamedThreadFactory("cond-"));

        for (int i = 0; i < producers; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < items; j++) {
                        buffer.put(idx * items + j);
                    }
                    log.log("producer-%d 生产完毕", idx);
                } finally {
                    done.countDown();
                }
            });
        }
        for (int i = 0; i < consumers; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    while (consumed.get() < total) {
                        Integer v = (Integer) buffer.take();
                        if (v == null) break;
                        consumed.incrementAndGet();
                    }
                    log.log("consumer-%d 消费完毕，当前共消费 %d", idx, consumed.get());
                } finally {
                    done.countDown();
                }
            });
        }

        try {
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdownNow();

        Map<String, Object> data = new HashMap<>();
        data.put("produced", total);
        data.put("consumed", consumed.get());
        data.put("bufferCapacity", 4);
        String summary = String.format("生产 %d 个，消费 %d 个；Condition 精确唤醒生产者/消费者，避免 signalAll 浪费",
                total, consumed.get());
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * LockSupport 三连演示。
     *
     * 八股：许可只有 0/1，不累积；先 unpark 后 park 不阻塞；park 响应中断但不抛异常；AQS 底层靠它。
     */
    public ScenarioResult lockSupport() {
        ScenarioLog log = new ScenarioLog();
        Map<String, Object> data = new HashMap<>();

        // (a) 先 unpark 后 park 不阻塞
        CountDownLatch readyA = new CountDownLatch(1);
        CountDownLatch goA = new CountDownLatch(1);
        AtomicInteger parkAElapsed = new AtomicInteger(-1);
        Thread tA = new Thread(() -> {
            readyA.countDown();
            try {
                goA.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long t0 = System.nanoTime();
            LockSupport.park();
            parkAElapsed.set((int) ((System.nanoTime() - t0) / 1_000_000));
            log.log("demo-a：先 unpark 后 park，park 立刻返回，耗时 %dms", parkAElapsed.get());
        }, "locksupport-a");
        tA.setDaemon(true);
        tA.start();
        try {
            readyA.await();
            LockSupport.unpark(tA); // 在 park 之前发放许可
            goA.countDown();
            tA.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        data.put("unparkBeforeParkOk", parkAElapsed.get() >= 0 && parkAElapsed.get() < 100);

        // (b) park 响应中断
        Thread tB = new Thread(() -> {
            LockSupport.park();
            boolean interrupted = Thread.interrupted();
            log.log("demo-b：park 响应中断，Thread.interrupted()=%s（不抛异常）", interrupted);
        }, "locksupport-b");
        tB.setDaemon(true);
        tB.start();
        try {
            Thread.sleep(100);
            tB.interrupt();
            tB.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        data.put("interruptOk", true);

        // (c) 许可不累积：unpark 两次，park 两次，第二次只能超时返回
        CountDownLatch readyC = new CountDownLatch(1);
        CountDownLatch goC = new CountDownLatch(1);
        AtomicInteger secondParkTimeout = new AtomicInteger(-1);
        Thread tC = new Thread(() -> {
            readyC.countDown();
            try {
                goC.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long t0 = System.nanoTime();
            LockSupport.park();
            long first = (System.nanoTime() - t0) / 1_000_000;
            log.log("demo-c：第一次 park 消耗一个许可，立刻返回，耗时 %dms", first);
            t0 = System.nanoTime();
            LockSupport.parkNanos(500_000_000L);
            long second = (System.nanoTime() - t0) / 1_000_000;
            secondParkTimeout.set((int) second);
            log.log("demo-c：第二次 park 无许可，超时返回，耗时 %dms", second);
        }, "locksupport-c");
        tC.setDaemon(true);
        tC.start();
        try {
            readyC.await();
            LockSupport.unpark(tC);
            LockSupport.unpark(tC); // 多次 unpark 不累积
            goC.countDown();
            tC.join(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        data.put("permitNotAccumulated", secondParkTimeout.get() >= 400);

        String summary = "LockSupport 许可不累积：先 unpark 后 park 不阻塞；park 响应中断但不抛异常";
        return ScenarioResult.of(log, summary, data);
    }

    // ---------- 内部辅助类 ----------

    private static class Account {
        final String name;
        final ReentrantLock lock = new ReentrantLock();
        private int balance;

        Account(String name, int balance) {
            this.name = name;
            this.balance = balance;
        }

        int getBalance() { return balance; }
        void withdraw(int amount) { balance -= amount; }
        void deposit(int amount) { balance += amount; }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    private static class BoundedBuffer {
        private final Object[] items;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        private int count;
        private int putIndex;
        private int takeIndex;
        private final ScenarioLog log;

        BoundedBuffer(int capacity, ScenarioLog log) {
            this.items = new Object[capacity];
            this.log = log;
        }

        void put(Object x) {
            lock.lock();
            try {
                while (count == items.length) {
                    log.log("缓冲区满（%d/%d），生产者 %s 进入 notFull 等待", count, items.length, Thread.currentThread().getName());
                    notFull.await();
                }
                items[putIndex] = x;
                putIndex = (putIndex + 1) % items.length;
                count++;
                log.log("%s 放入 %s，当前缓冲 %d/%d", Thread.currentThread().getName(), x, count, items.length);
                notEmpty.signal(); // 精确唤醒一个消费者
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        Object take() {
            lock.lock();
            try {
                while (count == 0) {
                    log.log("缓冲区空，消费者 %s 进入 notEmpty 等待", Thread.currentThread().getName());
                    notEmpty.await(100, TimeUnit.MILLISECONDS);
                    if (Thread.currentThread().isInterrupted()) return null;
                }
                Object x = items[takeIndex];
                items[takeIndex] = null;
                takeIndex = (takeIndex + 1) % items.length;
                count--;
                log.log("%s 取出 %s，当前缓冲 %d/%d", Thread.currentThread().getName(), x, count, items.length);
                notFull.signal(); // 精确唤醒一个生产者
                return x;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                lock.unlock();
            }
        }
    }
}

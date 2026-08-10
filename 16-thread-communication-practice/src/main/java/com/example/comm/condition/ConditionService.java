package com.example.comm.condition;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 03. 基于锁对象 / 等待通知：Condition（Lock 的等待队列）。
 *
 * wait/notify 的升级版：一个锁可以 newCondition() 出多个条件队列，
 * 可以「精准唤醒某一类等待者」——这是 synchronized 的单个 WaitSet 做不到的。
 * JUC 的 ArrayBlockingQueue、AQS 的条件队列都基于它。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final CommLogStore logStore;

    /**
     * Condition 有界缓冲：一个 ReentrantLock + 两个 Condition。
     *
     * - notEmpty：消费者等它（空时 await），生产者放货后 signal；
     * - notFull：生产者等它（满时 await），消费者取货后 signal。
     *
     * 与 wait/notify 版本对比：不用 notifyAll 惊群，signal 只唤醒正确那一边。
     */
    public Map<String, Object> boundedBuffer(int productions, int capacity) {
        int safeProductions = Math.max(1, Math.min(productions, 2000));
        int safeCapacity = Math.max(1, Math.min(capacity, 100));
        Map<String, Object> result = new LinkedHashMap<>();

        ReentrantLock lock = new ReentrantLock();
        Condition notFull = lock.newCondition();
        Condition notEmpty = lock.newCondition();
        int[] items = new int[safeCapacity];
        int[] count = {0};
        int[] putIndex = {0};
        int[] takeIndex = {0};
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        AtomicInteger maxBuffered = new AtomicInteger();

        Runnable produce = () -> {
            for (int i = 0; i < safeProductions; i++) {
                lock.lock();
                try {
                    while (count[0] == items.length) {
                        notFull.await();
                    }
                    items[putIndex[0]] = i;
                    putIndex[0] = (putIndex[0] + 1) % items.length;
                    count[0]++;
                    maxBuffered.accumulateAndGet(count[0], Math::max);
                    notEmpty.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    lock.unlock();
                }
                produced.incrementAndGet();
            }
        };
        Runnable consume = () -> {
            for (int i = 0; i < safeProductions; i++) {
                lock.lock();
                try {
                    while (count[0] == 0) {
                        notEmpty.await();
                    }
                    takeIndex[0] = (takeIndex[0] + 1) % items.length;
                    count[0]--;
                    notFull.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    lock.unlock();
                }
                consumed.incrementAndGet();
            }
        };

        Thread p = new Thread(produce, "condition-producer");
        Thread c = new Thread(consume, "condition-consumer");
        long start = System.nanoTime();
        p.start();
        c.start();
        try {
            p.join();
            c.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("capacity", safeCapacity);
        result.put("productions", safeProductions);
        result.put("produced", produced.get());
        result.put("consumed", consumed.get());
        result.put("balanced", produced.get() == safeProductions && consumed.get() == safeProductions);
        result.put("maxBuffered", maxBuffered.get());
        result.put("totalMs", totalMs);
        result.put("tip", "notFull 管「还能放吗」，notEmpty 管「还有吗」：生产者只等 notFull、消费者只等 notEmpty，"
                + "signal 精准唤醒对方，不会像 notifyAll 那样惊群。");

        logStore.add("condition", "bounded-buffer", 2, produced.get() == safeProductions, "Condition 有界缓冲");
        return result;
    }

    /**
     * signal 精准唤醒：waiters 个线程分为「偶数组 / 奇数组」，各自 await 在独立 Condition 上。
     *
     * 主线程只 signal 偶数组 → 只有偶数组被唤醒、奇数组继续沉睡。
     * 同样的场景用 wait/notifyAll 只能全部唤醒（无法只叫醒某一类）。
     */
    public Map<String, Object> signalDemo(int waiters) {
        int safeWaiters = Math.max(2, Math.min(waiters, 16));
        Map<String, Object> result = new LinkedHashMap<>();

        ReentrantLock lock = new ReentrantLock();
        Condition evenCondition = lock.newCondition();
        Condition oddCondition = lock.newCondition();
        AtomicInteger evenWoken = new AtomicInteger();
        AtomicInteger oddWoken = new AtomicInteger();
        AtomicInteger evenWaiting = new AtomicInteger();
        AtomicInteger oddWaiting = new AtomicInteger();
        AtomicInteger stillWaiting = new AtomicInteger();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < safeWaiters; i++) {
            final boolean even = (i % 2 == 0);
            Thread t = new Thread(() -> {
                lock.lock();
                try {
                    if (even) {
                        evenWaiting.incrementAndGet();
                        evenCondition.await();
                        evenWoken.incrementAndGet();
                    } else {
                        oddWaiting.incrementAndGet();
                        oddCondition.await();
                        oddWoken.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }, "condition-waiter-" + i);
            threads.add(t);
            t.start();
        }

        // 等所有线程都进入各自的等待队列
        sleepUntil(() -> evenWaiting.get() + oddWaiting.get() >= safeWaiters, 3000);
        int waitingBefore = evenWaiting.get() + oddWaiting.get();

        // 只 signal 偶数组：精准唤醒
        lock.lock();
        try {
            for (int i = 0; i < evenWaiting.get(); i++) {
                evenCondition.signal();
            }
        } finally {
            lock.unlock();
        }
        sleep(100);

        result.put("waiters", safeWaiters);
        result.put("waitingBefore", waitingBefore);
        result.put("evenWoken", evenWoken.get());
        result.put("oddWoken", oddWoken.get());
        result.put("stillSleeping", safeWaiters - evenWoken.get() - oddWoken.get());
        result.put("tip", "只 signal 了偶数组 → 偶数被唤醒 " + evenWoken.get() + " 个，奇数仍沉睡 "
                + (safeWaiters - evenWoken.get() - oddWoken.get()) + " 个：Condition 可以精准唤醒某一类等待者。");

        // 收尾：唤醒剩余的奇数组，避免线程悬挂
        lock.lock();
        try {
            oddCondition.signalAll();
            evenCondition.signalAll();
        } finally {
            lock.unlock();
        }
        for (Thread t : threads) {
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logStore.add("condition", "signal-demo", safeWaiters, evenWoken.get() > 0 && oddWoken.get() == 0, "signal 精准唤醒");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vsWaitNotify", new LinkedHashMap<String, Object>() {{
            put("多队列", "一个 Lock 可以 newCondition() 多个条件队列（notFull/notEmpty...），wait/notify 每个对象只有一个 WaitSet");
            put("精准唤醒", "signal 只唤醒当前 Condition 上的一个等待者，不惊群；notify 无法指定唤醒哪一类");
            put("可中断/可超时", "await(ms)/awaitNanos 支持超时，lockInterruptibly 支持响应中断，wait 只能抛 InterruptedException");
            put("绑定", "Condition 必须由 Lock 创建（lock.newCondition()），本质是 AQS 的条件队列");
        }});
        result.put("signalVsSignalAll", new LinkedHashMap<String, Object>() {{
            put("signal", "只唤醒一个，精准但若唤醒的是「同类」，可能只推进一个任务；多消费者时要注意");
            put("signalAll", "唤醒该条件队列全部，安全但可能惊群（多个被唤醒者抢锁后重新判断）");
            put("实践", "ArrayBlockingQueue 里 put 后 signal、take 后 signal，因为每次只产生一个空位/一个数据，signal 就够");
        }});
        result.put("whyWhile", "和 wait 一样：await 返回后条件可能又被别人改掉（竞争 + 虚假唤醒），必须 while 重新检查");
        result.put("aqs", "LockSupport 管阻塞/唤醒，AQS 的 ConditionObject 用单向条件队列保存等待者；JUC 一切同步器的地基");
        result.put("tip", "面试：wait/notify 是「一个队列、广播式」；Condition 是「一个锁、多个队列、点对点」，这就是它更高级的原因。");
        return result;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepUntil(java.util.function.BooleanSupplier condition, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            sleep(5);
        }
    }
}

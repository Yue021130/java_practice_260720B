package com.example.comm.waitnotify;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 02. 基于锁对象 / 等待通知：Object 的 wait / notify / notifyAll。
 *
 * 本质是「每个对象自带的等待队列」：线程在持有 monitor 时调用 wait() 释放锁并
 * 进入该对象的等待队列，其它线程在同一 monitor 上调用 notify()/notifyAll() 唤醒。
 * 经典用法就是生产者-消费者模型。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitNotifyService {

    private final CommLogStore logStore;

    /**
     * wait/notify 生产者-消费者：一个有界环形缓冲 + 一个对象锁。
     *
     * - 生产者：缓冲满时 wait()（释放锁），放入数据后 notifyAll()；
     * - 消费者：缓冲空时 wait()（释放锁），取走数据后 notifyAll()。
     *
     * 演示记录生产数、消费数、缓冲峰值占用，以及每一步「谁在等 / 谁被唤醒」。
     */
    public Map<String, Object> producerConsumer(int productions, int capacity) {
        int safeProductions = Math.max(1, Math.min(productions, 2000));
        int safeCapacity = Math.max(1, Math.min(capacity, 100));
        Map<String, Object> result = new LinkedHashMap<>();

        BoundedBuffer buffer = new BoundedBuffer(safeCapacity);
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        AtomicInteger maxBuffered = new AtomicInteger();

        Thread producer = new Thread(() -> {
            for (int i = 0; i < safeProductions; i++) {
                buffer.put(i);
                produced.incrementAndGet();
                maxBuffered.accumulateAndGet(buffer.size(), Math::max);
            }
        }, "producer");
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < safeProductions; i++) {
                buffer.take();
                consumed.incrementAndGet();
            }
        }, "consumer");

        long start = System.nanoTime();
        producer.start();
        consumer.start();
        try {
            producer.join();
            consumer.join();
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
        result.put("tip", "满/空时 wait 释放锁让对方干活，放/取后 notifyAll 唤醒对方：生产=" + produced.get()
                + "，消费=" + consumed.get() + "，缓冲峰值占用=" + maxBuffered.get() + "/" + safeCapacity + "。");

        logStore.add("waitnotify", "producer-consumer", 2, produced.get() == safeProductions,
                "wait/notify 生产消费");
        return result;
    }

    /**
     * 有界缓冲：wait/notify 的教科书实现。
     */
    static class BoundedBuffer {

        private final Object lock = new Object();
        private final int[] items;
        private int count, putIndex, takeIndex;

        BoundedBuffer(int capacity) {
            items = new int[capacity];
        }

        void put(int value) {
            synchronized (lock) {
                // 为什么用 while 不用 if：被唤醒后要「重新检查条件」（虚假唤醒 / 多个生产者竞争）
                while (count == items.length) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                items[putIndex] = value;
                putIndex = (putIndex + 1) % items.length;
                count++;
                lock.notifyAll();   // 通知可能在等的消费者
            }
        }

        int take() {
            synchronized (lock) {
                while (count == 0) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                }
                int value = items[takeIndex];
                takeIndex = (takeIndex + 1) % items.length;
                count--;
                lock.notifyAll();   // 通知可能在等空位的生产者
                return value;
            }
        }

        int size() {
            synchronized (lock) {
                return count;
            }
        }
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("whySynchronized", "wait/notify 必须持有该对象的 monitor（锁）才能调用，否则抛 IllegalMonitorStateException："
                + "wait 要「持有锁 → 释放锁 → 进等待队列」，notify 要「持有锁 → 唤醒」，锁是它们安全协作的前提");
        result.put("whyWhileNotIf", new String[]{
                "虚假唤醒（spurious wakeup）：wait 返回时条件未必已满足（系统/平台层面可能莫名唤醒），需要再查一次条件",
                "多线程竞争：notifyAll 唤醒的线程都要抢锁，先抢到的可能把资源又消耗光，后来的必须重新 wait",
                "标准姿势：while (条件不满足) lock.wait(); —— 唤醒后回到 while 重新判断，而不是 if 只判断一次"
        });
        result.put("notifyVsNotifyAll", new LinkedHashMap<String, Object>() {{
            put("notify", "只唤醒等待队列里的一个线程（随机/不指定）；唤醒的若是错误类型的等待者，可能没人补位 → 假死");
            put("notifyAll", "唤醒全部，让它们自行竞争与重新判断；安全但可能「惊群」");
            put("建议", "教科书都用 notifyAll；只有一个等待者时 notify 等价且更省");
        }});
        result.put("lostWakeup", "「先检查条件发现不满足，还没来得及 wait 就被置位」的窗口会丢唤醒 → 必须把「检查条件 + wait」放在同一个 synchronized 块内");
        result.put("memory", "本质 = 每个对象自带一个等待队列（monitor 的 WaitSet），一切线程通信都是「阻塞 + 等待队列 + 唤醒」的体现");
        result.put("tip", "面试：先讲 wait 释放锁进 WaitSet，notify 移出 WaitSet 去抢锁；再讲 while + 同一把锁，就齐了。");
        return result;
    }
}

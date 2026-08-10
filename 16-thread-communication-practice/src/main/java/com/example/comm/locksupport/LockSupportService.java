package com.example.comm.locksupport;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 05. 基于线程协作控制：LockSupport 的 park / unpark。
 *
 * JUC 的基石：AQS、FutureTask、线程池阻塞全是它。
 * 比 wait/notify 灵活在三点：
 * 1. 不需要先持有锁（wait 必须 synchronized 内）；
 * 2. 可以指定唤醒哪个线程（unpark(thread) 精确到人）；
 * 3. 信号可以「预发」：先 unpark 再 park 不会丢信号（permit 语义）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockSupportService {

    private final CommLogStore logStore;

    /**
     * park 后 unpark：worker 线程 park() 挂起，主线程延迟 delayMs 后 unpark 它。
     *
     * 记录 worker「从 park 到被唤醒」的等待耗时 ≈ delayMs。
     */
    public Map<String, Object> parkUnpark(int delayMs) {
        int safeDelayMs = Math.max(20, Math.min(delayMs, 5000));
        Map<String, Object> result = new LinkedHashMap<>();
        CountDownLatch parked = new CountDownLatch(1);
        long[] waitMs = {0};
        long[] parkedAt = {0};

        Thread worker = new Thread(() -> {
            parkedAt[0] = System.nanoTime();
            parked.countDown();
            LockSupport.park();                       // 挂起，等 permit
            waitMs[0] = (System.nanoTime() - parkedAt[0]) / 1_000_000;
        }, "park-worker");
        worker.start();

        try {
            parked.await(3, TimeUnit.SECONDS);        // 确保 worker 已进入 park
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sleep(safeDelayMs);
        LockSupport.unpark(worker);                   // 精确唤醒这个线程
        try {
            worker.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        result.put("delayMs", safeDelayMs);
        result.put("parkWaitMs", waitMs[0]);
        result.put("wokeByUnpark", !worker.isAlive());
        result.put("tip", "worker park 挂起约 " + safeDelayMs + "ms 后，unpark 精确唤醒它，实际等待 "
                + waitMs[0] + "ms：无需持锁、精确到线程，这是 wait/notify 做不到的。");

        logStore.add("locksupport", "park-unpark", 1, true, "park→unpark");
        return result;
    }

    /**
     * 先 unpark 后 park：permit 预发，park 立即通过。
     *
     * 若用 wait/notify 做同样顺序（先 notify 后 wait），唤醒信号会丢、
     * 线程永久挂死——这就是 LockSupport「信号预发」的核心优势。
     */
    public Map<String, Object> unparkFirst() {
        Map<String, Object> result = new LinkedHashMap<>();
        CountDownLatch ready = new CountDownLatch(1);
        long[] elapsedMs = {0};

        Thread worker = new Thread(() -> {
            ready.countDown();                        // 告诉主线程「我准备好 park 了」
            // 这里故意不先 park：等主线程先 unpark
            try {
                ready.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long t0 = System.nanoTime();
            LockSupport.park();                       // permit 已被预发 → 立即返回，不阻塞
            elapsedMs[0] = (System.nanoTime() - t0) / 1_000_000;
        }, "unpark-first-worker");
        worker.start();

        try {
            ready.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LockSupport.unpark(worker);                   // 先发 permit
        try {
            worker.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        result.put("elapsedMs", elapsedMs[0]);
        result.put("parkBypassed", !worker.isAlive());
        result.put("tip", "先 unpark（发一个 permit）再 park，park 读到预发的许可立即通过（只花 "
                + elapsedMs[0] + "ms）。同样的顺序换成 wait/notify 会永久死锁——这就是「信号预发」的价值。");

        logStore.add("locksupport", "unpark-first", 1, true, "permit 预发");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vsWaitNotify", new LinkedHashMap<String, Object>() {{
            put("无需持锁", "park/unpark 不需要先获得任何锁；wait/notify 必须 synchronized 内");
            put("精确唤醒", "unpark(thread) 指定唤醒哪个线程；notify 随机、notifyAll 广播");
            put("信号预发", "先 unpark 后 park 不丢信号（permit 最多累积 1 个）；先 notify 后 wait 必丢");
            put("可中断", "park 可被打断（抛 InterruptedException）；unpark 与 interrupt 语义相近");
        }});
        result.put("permit", "每个线程自带一个 permit（0 或 1）：unpark 把它置 1，park 看到 1 就消耗并通过、看到 0 就阻塞。"
                + "因为最多累计 1 个，连续两次 unpark 只等效一次——所以不能靠它做计数");
        result.put("whoUses", "AQS（acquireQueued 里 parkAndCheckInterrupt）、FutureTask、ForkJoinPool、线程池 worker 的阻塞都在用 LockSupport");
        result.put("tip", "面试：说清「park/unpark 三优势（无锁、指定线程、信号预发）」+「permit 最多 1 个」，再点一句 AQS 靠它实现阻塞唤醒。");
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

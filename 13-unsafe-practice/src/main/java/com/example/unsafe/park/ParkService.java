package com.example.unsafe.park;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 06. 线程阻塞与唤醒：park / unpark，LockSupport 的底层。
 *
 * <p>与 wait/notify 的本质区别：unpark 可以先于 park 调用（信号量“许可证”机制），
 * 且不需要持有任何锁、不会抛 InterruptedException 之外被吞掉的问题。
 * AQS（ReentrantLock / Semaphore / CountDownLatch）挂起线程用的就是它。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkService {

    private final Unsafe unsafe;

    /**
     * 两个现场演示：
     * ① 正常顺序：线程 park 住，主线程 300ms 后 unpark 唤醒；
     * ② 提前 unpark：先给许可证，线程再 park 会立即返回（这就是它比 wait/notify 强的地方）。
     */
    public Map<String, Object> demo() {
        List<Map<String, String>> timeline = Collections.synchronizedList(new ArrayList<>());

        // ---------- 场景一：正常 park -> unpark ----------
        Thread worker = new Thread(() -> {
            add(timeline, "【线程】启动，准备调用 unsafe.park");
            unsafe.park(false, 0); // 永久阻塞，直到被 unpark
            add(timeline, "【线程】park 返回：被主线程 unpark 唤醒");
        }, "park-worker");
        worker.start();
        sleep(300);
        add(timeline, "【主线程】已等待 300ms，调用 unsafe.unpark(worker)");
        unsafe.unpark(worker);
        join(worker);

        // ---------- 场景二：提前 unpark（许可证） ----------
        add(timeline, "---------- 场景二：先 unpark 再 park ----------");
        Thread early = new Thread(() -> {
            add(timeline, "【线程2】启动，立即调用 unsafe.park");
            unsafe.park(false, 0); // 许可证已就绪 → 直接返回，不会阻塞
            add(timeline, "【线程2】park 立即返回：证明许可证在 park 前已就绪");
        }, "park-early");
        add(timeline, "【主线程】先调用 unsafe.unpark(early)——给许可证");
        unsafe.unpark(early);
        early.start();
        join(early);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timeline", new ArrayList<>(timeline));
        result.put("tip", "park/unpark 等价于一个“许可证信号量”：unpark 给一次，park 消耗一次；"
                + "多次 unpark 只累积一张许可证。所以可以先 unpark 再 park，而 wait/notify 做不到。");
        return result;
    }

    /**
     * park/unpark 与 wait/notify 对比。
     */
    public Map<String, Object> compare() {
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row("使用条件", "无需任何锁", "必须在 synchronized 块内持有该对象的锁"));
        rows.add(row("顺序要求", "unpark 可以先于 park（许可证机制）", "wait 前必须 notify 已持有锁，且 notify 后线程仍需重新竞争锁"));
        rows.add(row("指定线程", "unpark(Thread) 精确唤醒指定线程", "notify 随机唤醒一个/notifyAll 全部，不能指定"));
        rows.add(row("超时", "park(boolean, long) 原生支持限时", "wait(long) 支持超时，但需配锁"));
        rows.add(row("中断", "park 会因中断返回，需自己检查", "wait 抛 InterruptedException"));
        rows.add(row("JUC 里的落地", "LockSupport.park/unpark → Unsafe.park/unpark", "老式代码 / Object 锁等待队列"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("tip", "一句话：wait/notify 是“对象级别”，park/unpark 是“线程级别”。");
        return result;
    }

    /**
     * 原理八股：LockSupport 与 AQS。
     */
    public Map<String, Object> explain() {
        List<Map<String, String>> items = new ArrayList<>();
        items.add(map("LockSupport", "JUC 对 Unsafe.park/unpark 的一层薄封装，还顺手处理了线程被设置中断位的情况"));
        items.add(map("AQS 怎么用", "acquire 拿不到锁时 LockSupport.park(this) 挂起；release 时 unpark(后继节点线程)——这是 ReentrantLock/Semaphore/CountDownLatch 的骨架"));
        items.add(map("许可证机制", "每个线程有一张“许可证”（Permit）：unpark 置 1，park 清 0 并阻塞；先 unpark 后 park 不阻塞"));
        items.add(map("为什么不用 wait", "wait/notify 要先抢锁、不能精确唤醒指定线程、时序敏感——AQS 队列里唤醒谁要精确定位"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("tip", "面试常问：LockSupport.park 与 wait 的区别？答出“许可证”“可指定线程”“无需锁”三点就稳了。");
        return result;
    }

    private void add(List<Map<String, String>> list, String msg) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("t", "t=" + (System.nanoTime() / 1_000_000) + "ms");
        m.put("event", msg);
        list.add(m);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void join(Thread t) {
        try {
            t.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, String> row(String k, String a, String b) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("item", k);
        m.put("parkUnpark", a);
        m.put("waitNotify", b);
        return m;
    }

    private Map<String, String> map(String k, String v) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("item", k);
        m.put("detail", v);
        return m;
    }
}

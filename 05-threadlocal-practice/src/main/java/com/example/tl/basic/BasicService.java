package com.example.tl.basic;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * ThreadLocal 基础原理演示。
 */
@Service
public class BasicService {

    /**
     * 线程隔离：两个线程对同一个 ThreadLocal 写不同值，互不影响。
     */
    public Map<String, Object> isolationDemo() throws InterruptedException {
        Map<String, Object> result = new HashMap<>();
        ThreadLocal<String> local = new ThreadLocal<>();
        List<String> logs = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            local.set("线程-A");
            logs.add(Thread.currentThread().getName() + " 写入: 线程-A, 读取: " + local.get());
            latch.countDown();
        }, "thread-A");

        Thread t2 = new Thread(() -> {
            local.set("线程-B");
            logs.add(Thread.currentThread().getName() + " 写入: 线程-B, 读取: " + local.get());
            latch.countDown();
        }, "thread-B");

        t1.start();
        t2.start();
        latch.await();

        result.put("logs", logs);
        result.put("note", "同一个 ThreadLocal 实例在不同线程的 ThreadLocalMap 中拥有独立 value");
        return result;
    }

    /**
     * initialValue / withInitial：未 set 时返回默认值。
     */
    public Map<String, Object> initialDemo() {
        Map<String, Object> result = new HashMap<>();

        ThreadLocal<String> local = new ThreadLocal<String>() {
            @Override
            protected String initialValue() {
                return "default-value";
            }
        };

        String beforeSet = local.get();
        local.set("custom-value");
        String afterSet = local.get();
        local.remove();
        String afterRemove = local.get();

        result.put("beforeSet", beforeSet);
        result.put("afterSet", afterSet);
        result.put("afterRemove", afterRemove);
        result.put("note", "initialValue() 在首次 get() 且未 set 时触发；remove 后再次 get 会重新调用 initialValue()");
        return result;
    }
}

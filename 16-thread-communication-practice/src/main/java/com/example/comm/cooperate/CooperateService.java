package com.example.comm.cooperate;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 04. 基于线程协作控制：Thread.join / interrupt 中断。
 *
 * - join：A 等 B 结束再继续，底层就是 wait(0) + isAlive() 轮询；
 * - interrupt：不是强制杀死，是「打招呼」——目标线程通过
 *   InterruptedException / isInterrupted() 感知后自行决定优雅退出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CooperateService {

    private final CommLogStore logStore;

    /**
     * join 演示：主线程 start 3 个子线程（各自模拟 taskMs 的耗时），再逐个 join。
     *
     * 子线程并行跑，所以总耗时 ≈ taskMs + 调度开销，而不是 3 × taskMs——
     * join 的意义就是「等它们都干完」，而不是串行化。
     */
    public Map<String, Object> joinDemo(int tasks, int taskMs) {
        int safeTasks = Math.max(1, Math.min(tasks, 16));
        int safeTaskMs = Math.max(10, Math.min(taskMs, 5000));
        Map<String, Object> result = new LinkedHashMap<>();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < safeTasks; i++) {
            Thread t = new Thread(() -> sleep(safeTaskMs), "join-task-" + i);
            threads.add(t);
            t.start();
        }

        long start = System.nanoTime();
        for (Thread t : threads) {
            try {
                t.join();   // 阻塞当前线程，等该线程执行完
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        result.put("tasks", safeTasks);
        result.put("taskMs", safeTaskMs);
        result.put("totalMs", totalMs);
        result.put("allDone", threads.stream().allMatch(t -> !t.isAlive()));
        result.put("tip", safeTasks + " 个任务并行跑 " + safeTaskMs + "ms，join 全部等完后总耗时 " + totalMs
                + "ms（≈taskMs 而非 " + safeTasks + "×taskMs）：join 只是「等齐」，不改变并行度。");

        logStore.add("cooperate", "join-demo", safeTasks, true, "join 等待完成");
        return result;
    }

    /**
     * interrupt 演示：两种「被打断」的感知方式。
     *
     * - mode=sleep：线程 sleep(2s) 被 interrupt → 抛 InterruptedException 立即退出，
     *   记录实际只睡了很短时间；
     * - mode=loop：线程 while(!isInterrupted()) 轮询干「活」，interrupt 后感知并退出，
     *   记录一共做了多少次迭代。
     *
     * 两种都是「协作式」：interrupt 只是设置中断标志，线程自己决定何时、如何退出。
     */
    public Map<String, Object> interruptDemo(String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        AtomicInteger iterations = new AtomicInteger();
        long[] elapsedMs = {0};

        Thread worker = new Thread(() -> {
            long t0 = System.nanoTime();
            if ("sleep".equalsIgnoreCase(mode)) {
                try {
                    Thread.sleep(2000);            // 阻塞方法：被打断会抛异常
                } catch (InterruptedException e) {
                    elapsedMs[0] = (System.nanoTime() - t0) / 1_000_000;
                    result.put("exitWay", "InterruptedException");
                    result.put("wasInterrupted", Thread.currentThread().isInterrupted());
                    return;
                }
                result.put("exitWay", "自然睡醒（未被打断）");
            } else {
                int count = 0;
                while (!Thread.currentThread().isInterrupted()) {   // 非阻塞：自己轮询标志
                    count++;
                    if (count % 10_000 == 0) {
                        iterations.set(count);
                    }
                }
                elapsedMs[0] = (System.nanoTime() - t0) / 1_000_000;
                result.put("exitWay", "isInterrupted() 感知退出");
                result.put("wasInterrupted", Thread.currentThread().isInterrupted());
                iterations.set(count);
            }
        }, "interrupt-worker");
        worker.start();

        // 等线程真正进入 sleep / 循环，再打断它
        sleep(150);
        long interruptAt = System.nanoTime();
        worker.interrupt();
        try {
            worker.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        result.put("mode", mode);
        result.put("interrupted", true);
        result.put("elapsedMs", elapsedMs[0]);
        result.put("interruptLatencyMs", (System.nanoTime() - interruptAt) / 1_000_000);
        if (iterations.get() > 0) {
            result.put("iterations", iterations.get());
        }
        result.put("tip", "interrupt() 只是设置中断标志：阻塞方法（sleep/wait/join）会立刻抛 InterruptedException 并清标志，"
                + "非阻塞循环要用 isInterrupted() 自己感知。线程从「被 interrupt 到退出」只花了 "
                + ((System.nanoTime() - interruptAt) / 1_000_000) + "ms，是协作退出不是强杀。");

        logStore.add("cooperate", "interrupt-demo", 1, true, "interrupt " + mode);
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("join", new LinkedHashMap<String, Object>() {{
            put("作用", "A 线程调用 b.join()：A 阻塞，等 b 执行完再继续；常用于「聚合子任务结果」");
            put("底层", "jdk8 的 join() 是 while(isAlive()) wait(0) —— 0 表示无限等；本质是 Object 等待队列 + 轮询");
            put("带超时", "join(ms)：最多等 ms 毫秒，超时继续走，避免永久卡死");
        }});
        result.put("interrupt", new LinkedHashMap<String, Object>() {{
            put("本质", "协作机制不是强制杀死：interrupt() 只设置线程的中断标志位，目标线程自己决定何时退出");
            put("阻塞方法", "sleep / wait / join / 锁获取 遇到中断 → 抛 InterruptedException 并清除中断标志");
            put("非阻塞", "循环里用 isInterrupted()（不清标志）/ Thread.interrupted()（读并清除标志）自行感知");
            put("最佳实践", "catch InterruptedException 后恢复标志位：Thread.currentThread().interrupt(); 再决定是否继续");
            put("注意", "中断处于阻塞态之外的状态（如正在跑 CPU 密集循环）不会立刻生效，需要自己检查标志");
        }});
        result.put("joinVsLatch", "join 与 CountDownLatch 都能「等 N 个线程」，但 Latch 更灵活：await 可超时、可在任意时刻等、不要求线程必须结束（countDown 即可）");
        result.put("tip", "面试：interrupt 三段式——为什么用中断（协作优雅）、怎么感知（异常/标志）、怎么恢复（重设标志）。");
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

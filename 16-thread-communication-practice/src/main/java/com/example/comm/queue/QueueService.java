package com.example.comm.queue;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 07. 基于阻塞队列：BlockingQueue 生产者-消费者标准解。
 *
 * 队列本身就是通信载体：天然解耦 + 背压（满时阻塞生产者）。
 * 内部用 ReentrantLock + Condition（notEmpty/notFull）实现，是对第 03 章
 * Condition 有界缓冲的直接封装——「处处是队列」在 Java 层面的标准实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final CommLogStore logStore;

    /**
     * put/take 阻塞演示：ArrayBlockingQueue 有界队列。
     *
     * - 生产者 put：队列满时阻塞，直到消费者取走腾出空位；
     * - 消费者 take：队列空时阻塞，直到生产者放入。
     *
     * 这就是「背压」：生产者不会被堆积压垮，只会放慢速度等消费者。
     */
    public Map<String, Object> blockingDemo(int productions, int capacity) {
        int safeProductions = Math.max(1, Math.min(productions, 2000));
        int safeCapacity = Math.max(1, Math.min(capacity, 100));
        Map<String, Object> result = new LinkedHashMap<>();
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(safeCapacity);
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        AtomicInteger maxSize = new AtomicInteger();
        AtomicInteger putWaits = new AtomicInteger();   // 生产者因满而阻塞的次数

        Thread producer = new Thread(() -> {
            for (int i = 0; i < safeProductions; i++) {
                try {
                    if (!queue.offer(i, 500, TimeUnit.MILLISECONDS)) {
                        putWaits.incrementAndGet();     // 满了，等 500ms 再试 → 模拟背压
                    }
                    // 用 offer(超时) 而非 put，避免极端场景下永久阻塞；正常路径等价 put
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                maxSize.accumulateAndGet(queue.size(), Math::max);
                produced.incrementAndGet();
            }
        }, "queue-producer");
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < safeProductions; i++) {
                try {
                    queue.take();                       // 空则阻塞等待
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                consumed.incrementAndGet();
            }
        }, "queue-consumer");

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
        result.put("maxSize", maxSize.get());
        result.put("putBlockedTimes", putWaits.get());
        result.put("totalMs", totalMs);
        result.put("tip", "容量=" + safeCapacity + "，缓冲峰值占用=" + maxSize.get() + "，生产者因满而放慢 "
                + putWaits.get() + " 次：队列把生产与消费解耦，满了自然背压，不会 OOM。");

        logStore.add("queue", "blocking-demo", 2, produced.get() == safeProductions, "BlockingQueue 背压");
        return result;
    }

    /**
     * 阻塞队列家族速览。
     */
    public Map<String, Object> family() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("members", new LinkedHashMap<String, Object>() {{
            put("ArrayBlockingQueue", "有界数组，先进先出；构造时定死容量；一把锁 + 两个 Condition；「最标准」的选型");
            put("LinkedBlockingQueue", "有向链表，默认无界（可指定容量）；两把锁（put/take 分离）并发更高；线程池默认用它");
            put("SynchronousQueue", "零容量，put 必须等 take 直接交接、不排队；Executors.newCachedThreadPool 用它（来了就开新线程）");
            put("PriorityBlockingQueue", "按优先级出队（可自定义比较器），无界；适合「任务按优先级调度」");
            put("DelayQueue", "元素到延迟时间才可出队；本质 PriorityQueue 按到期时间排；适合「定时任务/订单超时关闭」");
            put("LinkedTransferQueue", "支持 transfer 直接交接（无消费者则阻塞），更强的 Synchronous 版");
        }});
        result.put("methodGroups", new LinkedHashMap<String, Object>() {{
            put("抛异常", "add / remove / element —— 满或空时抛异常");
            put("返回特殊值", "offer / poll / peek —— 满或空时返回 false/null，不阻塞");
            put("阻塞", "put / take —— 满或空时一直阻塞");
            put("超时", "offer(e, time, unit) / poll(time, unit) —— 最多等多久，超时返回特殊值");
        }});
        result.put("tip", "四种行为按「是否阻塞 / 是否超时」记忆：add-offer-put-offer(超时) 一条线下来就全记住了。");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principle", "ArrayBlockingQueue 内部 = ReentrantLock + 两个 Condition（notEmpty / notFull）："
                + "take 空则 await notEmpty，put 满则 await notFull，操作后 signal 唤醒对方——就是第 03 章 Condition 有界缓冲的现成封装");
        result.put("backpressure", "背压 = 生产者速度 > 消费者速度时，put 会阻塞、排队积压被限住："
                + "系统不会无限堆积而 OOM，只会「慢下来」，这是消息中间件削峰填谷的同款思想");
        result.put("vsCondition", "自己用 Condition 写队列是「教学」；BlockingQueue 是「工程」：线程安全、边界处理、超时/中断全覆盖，"
                + "生产直接用它或它的变体");
        result.put("useCases", new String[]{
                "线程池任务队列：LinkedBlockingQueue / SynchronousQueue",
                "生产者-消费者解耦：ArrayBlockingQueue（有界）最常用",
                "优先级调度：PriorityBlockingQueue",
                "延迟任务：DelayQueue（订单超时关闭、定时重试）",
                "分布式削峰：MQ（Kafka/RabbitMQ）本质也是「跨进程的阻塞队列」"
        });
        result.put("tip", "面试：一句「阻塞队列 = 锁 + 条件队列 + 数组/链表」，一句「put/take 阻塞即背压」，再点 SynchronousQueue 零容量交接，就很能打。");
        return result;
    }
}

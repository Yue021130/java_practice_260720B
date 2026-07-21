package com.example.juc.container;

import com.example.juc.common.NamedThreadFactory;
import com.example.juc.common.ScenarioLog;
import com.example.juc.common.ScenarioResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发容器场景业务实现。
 *
 * 面试点：CHM 的 CAS + synchronized 锁头节点；CopyOnWrite 快照与弱一致；
 * DelayQueue 的 PriorityQueue + Leader-Follower；跳表无锁多层索引。
 */
@Slf4j
@Service
public class ContainerScenarioService {

    /**
     * HashMap 并发事故。
     *
     * 八股：JDK7 头插成环死循环；JDK8 尾插仍丢数据/覆盖；并发请用 CHM。
     */
    public ScenarioResult hashmapAccident(int threads, int keys) {
        ScenarioLog log = new ScenarioLog();
        Map<Integer, Integer> hashMap = new HashMap<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads, new NamedThreadFactory("hashmap-"));

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            final int threadIdx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < keys; j++) {
                        hashMap.put(threadIdx * keys + j, j);
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
            done.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long hashMapElapsed = System.currentTimeMillis() - t0;
        int expected = threads * keys;
        log.log("HashMap 结束，size=%d，期望=%d，耗时 %dms", hashMap.size(), expected, hashMapElapsed);

        // CHM 对照组
        ConcurrentHashMap<Integer, Integer> chm = new ConcurrentHashMap<>();
        CountDownLatch start2 = new CountDownLatch(1);
        CountDownLatch done2 = new CountDownLatch(threads);
        ExecutorService pool2 = Executors.newFixedThreadPool(threads, new NamedThreadFactory("chm-"));
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            final int threadIdx = i;
            pool2.submit(() -> {
                try {
                    start2.await();
                    for (int j = 0; j < keys; j++) {
                        chm.put(threadIdx * keys + j, j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done2.countDown();
                }
            });
        }
        start2.countDown();
        try {
            done2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long chmElapsed = System.currentTimeMillis() - t1;
        log.log("ConcurrentHashMap 结束，size=%d，期望=%d，耗时 %dms", chm.size(), expected, chmElapsed);

        pool.shutdownNow();
        pool2.shutdownNow();

        Map<String, Object> data = new HashMap<>();
        data.put("expected", expected);
        data.put("hashMapSize", hashMap.size());
        data.put("hashMapElapsedMs", hashMapElapsed);
        data.put("lost", expected - hashMap.size());
        data.put("chmSize", chm.size());
        data.put("chmElapsedMs", chmElapsed);
        return ScenarioResult.of(log, "HashMap 并发 put 会丢数据，ConcurrentHashMap 线程安全", data);
    }

    /**
     * ConcurrentHashMap 原子操作三件套。
     *
     * 八股：computeIfAbsent 保证 load 只一次；merge 做聚合；putIfAbsent 返回旧值可判断是否是首次放入。
     */
    public ScenarioResult chmOps() {
        ScenarioLog log = new ScenarioLog();
        Map<String, Object> data = new HashMap<>();

        // 1) putIfAbsent：首次放入成功，重复放入返回旧值
        ConcurrentHashMap<String, String> cache1 = new ConcurrentHashMap<>();
        String old = cache1.putIfAbsent("config", "value-loaded");
        String repeated = cache1.putIfAbsent("config", "value-reloaded");
        log.log("putIfAbsent 首次放入 old=%s，重复放入返回旧值=%s", old, repeated);
        data.put("putIfAbsentOldValue", repeated);

        // 2) computeIfAbsent：10 线程并发懒加载，load 函数只执行一次
        ConcurrentHashMap<String, String> cache2 = new ConcurrentHashMap<>();
        AtomicInteger loadCount = new AtomicInteger();
        int loadThreads = 10;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(loadThreads);
        ExecutorService pool = Executors.newFixedThreadPool(loadThreads, new NamedThreadFactory("chm-load-"));
        for (int i = 0; i < loadThreads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    cache2.computeIfAbsent("hotkey", k -> {
                        loadCount.incrementAndGet();
                        log.log("computeIfAbsent 执行回源加载 %s", k);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return "loaded-" + System.nanoTime();
                    });
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
        log.log("computeIfAbsent 并发加载次数=%d（应为 1，防缓存击穿）", loadCount.get());
        data.put("computeLoadCount", loadCount.get());

        // 3) merge：5 线程各提交 1000 个词，最终总词频=5000
        ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<>();
        String[] words = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape", "honeydew", "kiwi", "lemon",
                "mango", "nectarine", "orange", "papaya", "quince", "raspberry", "strawberry", "tangerine", "ugli", "vanilla"};
        int wordThreads = 5;
        int wordsPerThread = 1000;
        CountDownLatch start2 = new CountDownLatch(1);
        CountDownLatch done2 = new CountDownLatch(wordThreads);
        ExecutorService pool2 = Executors.newFixedThreadPool(wordThreads, new NamedThreadFactory("chm-merge-"));
        Random rand = new Random(42);
        for (int i = 0; i < wordThreads; i++) {
            pool2.submit(() -> {
                try {
                    start2.await();
                    for (int j = 0; j < wordsPerThread; j++) {
                        String w = words[rand.nextInt(words.length)];
                        wordCount.merge(w, 1, Integer::sum);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done2.countDown();
                }
            });
        }
        start2.countDown();
        try {
            done2.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool2.shutdownNow();
        int total = wordCount.values().stream().mapToInt(Integer::intValue).sum();
        log.log("merge 词频聚合：distinct=%d，total=%d（期望 %d）", wordCount.size(), total, wordThreads * wordsPerThread);
        data.put("distinctWords", wordCount.size());
        data.put("totalWordCount", total);

        return ScenarioResult.of(log, "CHM 提供 putIfAbsent/computeIfAbsent/merge 三种原子复合操作", data);
    }

    /**
     * CopyOnWriteArrayList 白名单。
     *
     * 八股：写时复制数组，读无锁；迭代器是快照，不会抛 CME；写多时内存翻倍。
     */
    public ScenarioResult cowWhitelist() {
        ScenarioLog log = new ScenarioLog();

        // CopyOnWriteArrayList 组
        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>(Arrays.asList("10.0.0.1", "10.0.0.2", "10.0.0.3"));
        int cowExceptions = runWhitelist(cow, "cow", log);

        // ArrayList 对照组
        List<String> arrayList = new ArrayList<>(Arrays.asList("10.0.0.1", "10.0.0.2", "10.0.0.3"));
        int arrayExceptions = runWhitelist(arrayList, "arraylist", log);

        Map<String, Object> data = new HashMap<>();
        data.put("cowIterations", 40);
        data.put("cowExceptions", cowExceptions);
        data.put("arrayListCmeCaught", arrayExceptions > 0);
        return ScenarioResult.of(log, "CopyOnWriteArrayList 遍历不抛 CME，ArrayList 会触发 ConcurrentModificationException", data);
    }

    private int runWhitelist(List<String> list, String tag, ScenarioLog log) {
        AtomicInteger exceptions = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);

        // 2 个读线程
        Thread r1 = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < 20; i++) {
                    for (String ip : list) {
                        Thread.sleep(5);
                    }
                }
                log.log("%s-reader-0 完成 20 次遍历，size 最终=%d", tag, list.size());
            } catch (ConcurrentModificationException e) {
                exceptions.incrementAndGet();
                log.log("%s-reader-0 触发 ConcurrentModificationException", tag);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, tag + "-reader-0");
        r1.setDaemon(true);

        Thread r2 = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < 20; i++) {
                    for (String ip : list) {
                        Thread.sleep(5);
                    }
                }
                log.log("%s-reader-1 完成 20 次遍历，size 最终=%d", tag, list.size());
            } catch (ConcurrentModificationException e) {
                exceptions.incrementAndGet();
                log.log("%s-reader-1 触发 ConcurrentModificationException", tag);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, tag + "-reader-1");
        r2.setDaemon(true);

        // 1 个写线程
        Thread w = new Thread(() -> {
            try {
                start.await();
                Thread.sleep(50);
                for (int i = 0; i < 5; i++) {
                    list.add("10.0.0." + (10 + i));
                    log.log("%s-writer 新增 IP 10.0.0.%d", tag, 10 + i);
                    Thread.sleep(30);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, tag + "-writer");
        w.setDaemon(true);

        r1.start();
        r2.start();
        w.start();
        start.countDown();
        try {
            done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return exceptions.get();
    }

    /**
     * DelayQueue 订单超时取消。
     *
     * 八股：内部 PriorityQueue 按到期时间排序；Leader-Follower 减少空转；
     * 生产替代：Redis ZSet 轮询、时间轮、MQ 延迟消息。
     */
    public ScenarioResult delayqueueOrder(int orders, int timeoutMs) {
        ScenarioLog log = new ScenarioLog();
        DelayQueue<DelayedOrder> queue = new DelayQueue<>();
        AtomicInteger cancelled = new AtomicInteger();
        long[] errors = new long[orders];

        // 下单，错开到期时间
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < orders; i++) {
            long delay = (long) timeoutMs * (i + 1) / orders;
            queue.offer(new DelayedOrder("order-" + i, delay));
            log.log("下单 %s，预计 %dms 后到期取消", "order-" + i, delay);
        }

        Thread consumer = new Thread(() -> {
            try {
                while (cancelled.get() < orders) {
                    DelayedOrder order = queue.take();
                    long actualDelay = System.currentTimeMillis() - order.createTime;
                    errors[cancelled.get()] = Math.abs(actualDelay - order.delay);
                    cancelled.incrementAndGet();
                    log.log("%s 到期取消，预期延迟 %dms，实际 %dms", order.id, order.delay, actualDelay);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "delayqueue-consumer");
        consumer.setDaemon(true);
        consumer.start();

        try {
            consumer.join(timeoutMs + orders * 200L + 2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long avgError = 0;
        for (long e : errors) avgError += e;
        avgError = cancelled.get() > 0 ? avgError / cancelled.get() : 0;

        Map<String, Object> data = new HashMap<>();
        data.put("orders", orders);
        data.put("cancelled", cancelled.get());
        data.put("avgDelayErrorMs", avgError);
        return ScenarioResult.of(log, String.format("%d 个订单到期，平均误差 %dms", cancelled.get(), avgError), data);
    }

    private static class DelayedOrder implements Delayed {
        final String id;
        final long delay;
        final long createTime;
        DelayedOrder(String id, long delayMs) {
            this.id = id;
            this.delay = delayMs;
            this.createTime = System.currentTimeMillis();
        }
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(createTime + delay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    /**
     * BlockingQueue 家族对比。
     *
     * 八股：线程池 workQueue 选型；SynchronousQueue 不存储直接交接；LinkedBlockingQueue 默认容量无限易 OOM。
     */
    public ScenarioResult blockingqueueFamily() {
        ScenarioLog log = new ScenarioLog();
        Map<String, Object> data = new HashMap<>();

        // ArrayBlockingQueue(2)：有界，第三个 offer 失败
        ArrayBlockingQueue<Integer> abq = new ArrayBlockingQueue<>(2);
        boolean abqOk1 = abq.offer(1);
        boolean abqOk2 = abq.offer(2);
        boolean abqOk3 = abq.offer(3);
        log.log("ArrayBlockingQueue(2)：offer 1=%s, 2=%s, 3=%s（第三个应失败）", abqOk1, abqOk2, abqOk3);
        data.put("arrayBlockingQueue", "容量 2，第三个 offer 失败 = " + !abqOk3);

        // LinkedBlockingQueue：默认容量 Integer.MAX_VALUE，演示有界感
        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue<>();
        log.log("LinkedBlockingQueue 默认容量=%d，线程池用它等于无界队列，容易 OOM", Integer.MAX_VALUE);
        data.put("linkedBlockingQueue", "默认容量 Integer.MAX_VALUE，阿里规约不推荐直接用于 newFixedThreadPool");

        // SynchronousQueue：不存储，必须配对
        SynchronousQueue<Integer> sq = new SynchronousQueue<>();
        boolean sqOfferWithoutConsumer = sq.offer(100);
        log.log("SynchronousQueue 无消费者时 offer=%s（直接失败，不存储元素）", sqOfferWithoutConsumer);
        Thread consumer = new Thread(() -> {
            try {
                Integer v = sq.take();
                log.log("SynchronousQueue 消费者 take 到 %d，配对成功", v);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "sq-consumer");
        consumer.setDaemon(true);
        consumer.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean sqOfferWithConsumer = sq.offer(200);
        log.log("SynchronousQueue 有消费者等待时 offer=%s", sqOfferWithConsumer);
        data.put("synchronousQueue", "不存储元素，put 必须等待 take 配对");

        // PriorityBlockingQueue：按优先级出队
        PriorityBlockingQueue<Integer> pbq = new PriorityBlockingQueue<>();
        pbq.offer(5);
        pbq.offer(1);
        pbq.offer(3);
        pbq.offer(2);
        pbq.offer(4);
        List<Integer> sorted = new ArrayList<>();
        while (!pbq.isEmpty()) {
            sorted.add(pbq.poll());
        }
        log.log("PriorityBlockingQueue 乱序入队 5,1,3,2,4，出队顺序=%s", sorted);
        data.put("priorityBlockingQueue", "出队顺序 " + sorted);

        try {
            consumer.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ScenarioResult.of(log, "四种阻塞队列特性同台：有界/无界/直接交接/优先级排序", data);
    }

    /**
     * 跳表排行榜。
     *
     * 八股：跳表多层索引 O(logN)；ConcurrentSkipListMap 用 CAS 无锁化，并发吞吐高；
     * TreeMap 必须外部加锁才能并发使用。
     */
    public ScenarioResult skiplistLeaderboard(int players) {
        ScenarioLog log = new ScenarioLog();
        ConcurrentSkipListMap<Double, String> leaderboard = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        ConcurrentHashMap<String, Double> playerScores = new ConcurrentHashMap<>();
        String[] playerNames = new String[players];
        for (int i = 0; i < players; i++) {
            playerNames[i] = "player-" + i;
            playerScores.put(playerNames[i], 0.0);
        }

        int updateThreads = 5;
        int updatesPerThread = 100;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(updateThreads);
        ExecutorService pool = Executors.newFixedThreadPool(updateThreads, new NamedThreadFactory("skiplist-"));
        Random rand = new Random(7);

        for (int i = 0; i < updateThreads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < updatesPerThread; j++) {
                        String player = playerNames[rand.nextInt(players)];
                        double delta = rand.nextInt(100);
                        playerScores.compute(player, (k, old) -> {
                            double nv = (old == null ? 0 : old) + delta;
                            // 移除旧分数（key 里加了玩家哈希保证唯一），插入新分数
                            leaderboard.remove(old + randDoubleHash(player));
                            leaderboard.put(nv + randDoubleHash(player), player);
                            return nv;
                        });
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
        pool.shutdownNow();

        // 取 Top3
        List<Map.Entry<Double, String>> top = new ArrayList<>(leaderboard.entrySet()).subList(0, Math.min(3, leaderboard.size()));
        List<String> top3 = new ArrayList<>();
        for (Map.Entry<Double, String> e : top) {
            String player = e.getValue();
            double score = playerScores.getOrDefault(player, 0.0);
            top3.add(String.format("%s=%.1f", player, score));
        }
        log.log("并发更新完成，Top3=%s", top3);

        Map<String, Object> data = new HashMap<>();
        data.put("totalUpdates", updateThreads * updatesPerThread);
        data.put("top3", top3);
        return ScenarioResult.of(log, String.format("%d 次并发更新完成，跳表实时 Top3=%s", updateThreads * updatesPerThread, top3), data);
    }

    private double randDoubleHash(String s) {
        // 把玩家名映射成极小的扰动，保证同分玩家 key 不冲突
        return (s.hashCode() & 0x7FFFFFFF) / 1_000_000_000_000.0;
    }
}

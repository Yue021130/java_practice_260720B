package com.example.cg.queue;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Queue / Deque 场景业务实现。
 *
 * 面试点：PriorityQueue 底层是小顶堆，O(logN) 插入/删除；ArrayDeque 无容量限制、不允许 null、比 LinkedList 快。
 */
@Service
public class QueueScenarioService {

    /**
     * PriorityQueue 取 TopK。
     */
    public ScenarioResult priorityqueueTopk(int k) {
        ScenarioLog log = new ScenarioLog();
        Random random = new Random(42);
        Map<String, Integer> wordCount = new HashMap<>();
        String[] words = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape"};
        for (int i = 0; i < 10000; i++) {
            String w = words[random.nextInt(words.length)];
            wordCount.merge(w, 1, Integer::sum);
        }
        log.log("生成 %d 个词的词频：%s", 10000, wordCount);

        // 小顶堆，堆顶是第 K 大的词里频率最小的
        PriorityQueue<Map.Entry<String, Integer>> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<String, Integer> e : wordCount.entrySet()) {
            if (minHeap.size() < k) {
                minHeap.offer(e);
            } else if (e.getValue() > minHeap.peek().getValue()) {
                minHeap.poll();
                minHeap.offer(e);
            }
        }

        String[] top = new String[minHeap.size()];
        for (int i = top.length - 1; i >= 0; i--) {
            Map.Entry<String, Integer> e = minHeap.poll();
            top[i] = e.getKey() + "=" + e.getValue();
        }
        log.log("Top%d=%s", k, java.util.Arrays.toString(top));

        Map<String, Object> data = new HashMap<>();
        data.put("k", k);
        data.put("topK", top);
        data.put("totalWords", 10000);
        return ScenarioResult.of(log, String.format("PriorityQueue 小顶堆在 O(NlogK) 内取出 Top%d", k), data);
    }

    /**
     * ArrayDeque 作为栈和队列。
     */
    public ScenarioResult arraydequeDual() {
        ScenarioLog log = new ScenarioLog();

        ArrayDeque<String> stack = new ArrayDeque<>();
        stack.push("first");
        stack.push("second");
        stack.push("third");
        log.log("栈 push 后：%s", stack);
        log.log("栈 pop：%s", stack.pop());
        log.log("栈 pop 后：%s", stack);

        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.offer("Alice");
        queue.offer("Bob");
        queue.offer("Carol");
        log.log("队列 offer 后：%s", queue);
        log.log("队列 poll：%s", queue.poll());
        log.log("队列 poll 后：%s", queue);

        Map<String, Object> data = new HashMap<>();
        data.put("stackAfterPop", stack);
        data.put("queueAfterPoll", queue);
        return ScenarioResult.of(log, "ArrayDeque 是首选双端队列：栈用 push/pop，队列用 offer/poll", data);
    }
}

package com.example.cg.realworld;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * 集合与泛型综合实战业务实现。
 *
 * 面试点：LRU = LinkedHashMap accessOrder + removeEldestEntry；
 * TopK = HashMap 计数 + 小顶堆；groupBy = Map.compute/merge；
 * 多字段排序 = Comparator.comparing.thenComparing.nullsFirst。
 */
@Service
public class RealWorldScenarioService {

    /**
     * LRU 缓存：容量淘汰。
     */
    public ScenarioResult lruCache(int capacity) {
        ScenarioLog log = new ScenarioLog();
        LinkedHashMap<String, String> cache = new LinkedHashMap<String, String>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                boolean remove = size() > capacity;
                if (remove) log.log("LRU 淘汰：%s", eldest.getKey());
                return remove;
            }
        };

        cache.put("user:1", "Alice");
        cache.put("user:2", "Bob");
        cache.put("user:3", "Carol");
        log.log("初始 put 后 keys=%s", cache.keySet());

        cache.get("user:1"); // 访问 user:1，变热
        cache.put("user:4", "Dave"); // 应淘汰最冷的 user:2
        log.log("访问 user:1 并 put user:4 后 keys=%s", cache.keySet());

        cache.put("user:5", "Eve"); // 淘汰下一个最冷的 user:3
        log.log("put user:5 后 keys=%s", cache.keySet());

        Map<String, Object> data = new HashMap<>();
        data.put("capacity", capacity);
        data.put("finalKeys", cache.keySet());
        return ScenarioResult.of(log, String.format("容量=%d 的 LRU 缓存，最近访问的保留，最久未使用的淘汰", capacity), data);
    }

    /**
     * TopK 热词统计。
     */
    public ScenarioResult topkWords(int k) {
        ScenarioLog log = new ScenarioLog();
        Random random = new Random(7);
        String[] words = {"java", "spring", "vue", "mysql", "redis", "kafka", "docker"};
        Map<String, Integer> freq = new HashMap<>();
        for (int i = 0; i < 5000; i++) {
            freq.merge(words[random.nextInt(words.length)], 1, Integer::sum);
        }
        log.log("5000 次随机词频=%s", freq);

        PriorityQueue<Map.Entry<String, Integer>> heap =
                new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            if (heap.size() < k) {
                heap.offer(e);
            } else if (e.getValue() > heap.peek().getValue()) {
                heap.poll();
                heap.offer(e);
            }
        }

        List<String> top = new ArrayList<>();
        while (!heap.isEmpty()) {
            Map.Entry<String, Integer> e = heap.poll();
            top.add(0, e.getKey() + "=" + e.getValue());
        }
        log.log("Top%d=%s", k, top);

        Map<String, Object> data = new HashMap<>();
        data.put("k", k);
        data.put("topK", top);
        return ScenarioResult.of(log, String.format("HashMap 词频 + PriorityQueue 小顶堆取 Top%d 热词", k), data);
    }

    /**
     * groupBy 订单：按用户/状态聚合。
     */
    public ScenarioResult groupby() {
        ScenarioLog log = new ScenarioLog();
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(1L, "PAID", 100));
        orders.add(new Order(1L, "PAID", 200));
        orders.add(new Order(2L, "UNPAID", 150));
        orders.add(new Order(2L, "PAID", 50));
        orders.add(new Order(3L, "UNPAID", 300));

        // 按用户分组：总金额、订单数
        Map<Long, UserSummary> byUser = new HashMap<>();
        for (Order o : orders) {
            byUser.compute(o.userId, (k, v) -> {
                if (v == null) return new UserSummary(o.amount, 1);
                v.total += o.amount;
                v.count++;
                return v;
            });
        }
        log.log("按用户聚合=%s", byUser);

        // 按状态分组：订单列表
        Map<String, List<Order>> byStatus = new HashMap<>();
        for (Order o : orders) {
            byStatus.computeIfAbsent(o.status, k -> new ArrayList<>()).add(o);
        }
        log.log("按状态分组=%s", byStatus);

        Map<String, Object> data = new HashMap<>();
        data.put("byUser", byUser);
        data.put("byStatus", byStatus);
        return ScenarioResult.of(log, "Map.compute / computeIfAbsent / merge 是分组聚合的利器", data);
    }

    /**
     * Comparator 多字段排序 + null 处理。
     */
    public ScenarioResult comparatorSort() {
        ScenarioLog log = new ScenarioLog();
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("Alice", "Engineering", 30));
        employees.add(new Employee("Bob", "Sales", null));
        employees.add(new Employee("Alice", "Engineering", 25));
        employees.add(new Employee("Charlie", null, 35));

        employees.sort(Comparator
                .comparing(Employee::getDept, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Employee::getName)
                .thenComparing(Employee::getAge, Comparator.nullsLast(Comparator.naturalOrder())));
        log.log("多字段排序（nullsFirst/nullsLast）：%s", employees);

        Map<String, Object> data = new HashMap<>();
        data.put("sorted", employees);
        return ScenarioResult.of(log, "Comparator.comparing + thenComparing + nullsFirst/nullsLast 处理复杂排序", data);
    }

    // ---------- 内部类 ----------

    private static class Order {
        private final Long userId;
        private final String status;
        private final int amount;
        Order(Long userId, String status, int amount) {
            this.userId = userId;
            this.status = status;
            this.amount = amount;
        }
        public Long getUserId() { return userId; }
        public String getStatus() { return status; }
        public int getAmount() { return amount; }
        @Override public String toString() {
            return String.format("Order(%d,%s,%d)", userId, status, amount);
        }
    }

    private static class UserSummary {
        private int total;
        private int count;
        UserSummary(int total, int count) { this.total = total; this.count = count; }
        public int getTotal() { return total; }
        public int getCount() { return count; }
        @Override public String toString() { return "Summary{total=" + total + ",count=" + count + "}"; }
    }

    private static class Employee {
        private final String name;
        private final String dept;
        private final Integer age;
        Employee(String name, String dept, Integer age) { this.name = name; this.dept = dept; this.age = age; }
        public String getName() { return name; }
        public String getDept() { return dept; }
        public Integer getAge() { return age; }
        @Override public String toString() { return name + "[" + dept + "," + age + "]"; }
    }
}

package com.example.cg.generics;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 泛型场景业务实现。
 *
 * 面试点：PECS（Producer Extends Consumer Super）、类型擦除、泛型方法、泛型 DAO、
 * 数组不能创建泛型、raw type 破坏类型安全。
 */
@Service
public class GenericsScenarioService {

    /**
     * PECS 原则演示。
     */
    public ScenarioResult pecs() {
        ScenarioLog log = new ScenarioLog();

        // Producer Extends：只能读，不能写（除了 null）
        List<? extends Number> producer = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
        Number first = producer.get(0);
        log.log("List<? extends Number> get 到 %s（类型是 Number）", first);

        boolean writeCaught = false;
        try {
            // 编译错误：producer.add(1); 但运行时可通过反射绕过，这里直接说明
            log.log("producer.add(1) 在编译期报错：无法确定具体子类型");
        } catch (Exception e) {
            writeCaught = true;
        }

        // Consumer Super：可以写入 Integer，但只能按 Object 读取
        List<? super Integer> consumer = new ArrayList<Number>();
        consumer.add(1);
        consumer.add(2);
        Object o = consumer.get(0);
        log.log("List<? super Integer> 可 add Integer，get 返回 Object=%s", o);

        // 实际应用：copy 方法
        List<Integer> src = Arrays.asList(10, 20, 30);
        List<Number> dest = new ArrayList<>();
        copy(src, dest);
        log.log("copy(List<? extends T>, List<? super T>) 从 Integer 列表复制到 Number 列表：%s", dest);

        Map<String, Object> data = new HashMap<>();
        data.put("producerFirst", first);
        data.put("consumerSize", consumer.size());
        data.put("copiedDest", dest);
        return ScenarioResult.of(log, "PECS：extends 用于读（producer），super 用于写（consumer）", data);
    }

    private <T> void copy(List<? extends T> src, List<? super T> dest) {
        dest.addAll(src);
    }

    /**
     * 类型擦除与 raw type。
     */
    public ScenarioResult typeErasure() {
        ScenarioLog log = new ScenarioLog();

        List<String> strings = new ArrayList<>();
        strings.add("hello");
        log.log("编译期 List<String>，运行期擦除为 List，泛型信息不可通过反射获取（局部变量）");

        // raw type：失去类型检查
        List raw = new ArrayList();
        raw.add("string");
        raw.add(123);
        boolean mixed = false;
        try {
            for (Object obj : raw) {
                if (obj instanceof Integer) {
                    mixed = true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        log.log("raw type List 可同时放入 String 和 Integer，运行时才可能暴露问题=%s", mixed);

        Map<String, Object> data = new HashMap<>();
        data.put("rawTypeAllowsMixed", mixed);
        data.put("note", "Java 泛型是编译期类型检查，运行时擦除");
        return ScenarioResult.of(log, "泛型在运行时擦除为 Object/边界；raw type 破坏编译期类型安全", data);
    }

    /**
     * 泛型方法与类型推断。
     */
    public ScenarioResult genericMethod() {
        ScenarioLog log = new ScenarioLog();

        List<Integer> ints = Arrays.asList(3, 1, 4, 1, 5, 9);
        Integer maxInt = findMax(ints);
        log.log("Integer 列表 %s 的最大值=%d", ints, maxInt);

        List<String> strs = Arrays.asList("banana", "apple", "cherry");
        String maxStr = findMax(strs);
        log.log("String 列表 %s 的最大值=%s", strs, maxStr);

        Map<String, Object> data = new HashMap<>();
        data.put("maxInt", maxInt);
        data.put("maxString", maxStr);
        return ScenarioResult.of(log, "泛型方法 <T extends Comparable<T>> 可同时处理 Integer、String 等可比较类型", data);
    }

    private <T extends Comparable<T>> T findMax(List<T> list) {
        T max = list.get(0);
        for (T t : list) {
            if (t.compareTo(max) > 0) max = t;
        }
        return max;
    }

    /**
     * 泛型 DAO / Service。
     */
    public ScenarioResult genericDao() {
        ScenarioLog log = new ScenarioLog();

        BaseRepository<User, Long> userRepo = new UserRepository();
        userRepo.save(new User(1L, "Alice"));
        User user = userRepo.findById(1L);
        log.log("泛型 DAO UserRepository 保存并查询：%s", user);

        BaseRepository<Order, String> orderRepo = new OrderRepository();
        orderRepo.save(new Order("O-1001", 199));
        Order order = orderRepo.findById("O-1001");
        log.log("泛型 DAO OrderRepository 保存并查询：%s", order);

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("order", order);
        return ScenarioResult.of(log, "BaseRepository<T, ID> 让 DAO 层复用 CRUD 模板，同时保持类型安全", data);
    }

    // ---------- 内部泛型类 ----------

    private interface BaseEntity<ID> {
        ID getId();
    }

    private interface BaseRepository<T extends BaseEntity<ID>, ID> {
        void save(T entity);
        T findById(ID id);
    }

    private static class User implements BaseEntity<Long> {
        private final Long id;
        private final String name;
        User(Long id, String name) { this.id = id; this.name = name; }
        @Override public Long getId() { return id; }
        public String getName() { return name; }
        @Override public String toString() { return "User{" + id + "," + name + "}"; }
    }

    private static class Order implements BaseEntity<String> {
        private final String id;
        private final int amount;
        Order(String id, int amount) { this.id = id; this.amount = amount; }
        @Override public String getId() { return id; }
        public int getAmount() { return amount; }
        @Override public String toString() { return "Order{" + id + "," + amount + "}"; }
    }

    private static class UserRepository implements BaseRepository<User, Long> {
        private final Map<Long, User> store = new HashMap<>();
        @Override public void save(User user) { store.put(user.getId(), user); }
        @Override public User findById(Long id) { return store.get(id); }
    }

    private static class OrderRepository implements BaseRepository<Order, String> {
        private final Map<String, Order> store = new HashMap<>();
        @Override public void save(Order order) { store.put(order.getId(), order); }
        @Override public Order findById(String id) { return store.get(id); }
    }
}

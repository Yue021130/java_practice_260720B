package com.example.cg.set;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Set 场景业务实现。
 *
 * 面试点：Set 去重依赖 equals/hashCode；LinkedHashSet 保序；TreeSet 排序（红黑树 O(logN)）。
 */
@Service
public class SetScenarioService {

    /**
     * HashSet 去重：未重写 equals/hashCode 的自定义对象无法去重。
     */
    public ScenarioResult hashsetDedup() {
        ScenarioLog log = new ScenarioLog();

        Set<BadPerson> badSet = new HashSet<>();
        badSet.add(new BadPerson("Alice", 20));
        badSet.add(new BadPerson("Alice", 20));
        badSet.add(new BadPerson("Bob", 25));
        log.log("未重写 equals/hashCode，size=%d（期望 2，实际 %d）", badSet.size(), badSet.size());

        Set<GoodPerson> goodSet = new HashSet<>();
        goodSet.add(new GoodPerson("Alice", 20));
        goodSet.add(new GoodPerson("Alice", 20));
        goodSet.add(new GoodPerson("Bob", 25));
        log.log("重写 equals/hashCode 后，size=%d（正确去重）", goodSet.size());

        Map<String, Object> data = new HashMap<>();
        data.put("badSetSize", badSet.size());
        data.put("goodSetSize", goodSet.size());
        return ScenarioResult.of(log, "HashSet 去重依赖 equals/hashCode；未重写则对象引用不同即不同", data);
    }

    /**
     * LinkedHashSet 保持插入顺序。
     */
    public ScenarioResult linkedhashsetOrder() {
        ScenarioLog log = new ScenarioLog();

        Set<String> hashSet = new HashSet<>();
        hashSet.add("first");
        hashSet.add("second");
        hashSet.add("third");
        log.log("HashSet 遍历顺序（不保证插入顺序）：%s", hashSet);

        Set<String> linkedSet = new LinkedHashSet<>();
        linkedSet.add("first");
        linkedSet.add("second");
        linkedSet.add("third");
        log.log("LinkedHashSet 遍历顺序（保持插入顺序）：%s", linkedSet);

        Map<String, Object> data = new HashMap<>();
        data.put("hashSetOrder", hashSet);
        data.put("linkedHashSetOrder", linkedSet);
        return ScenarioResult.of(log, "需要保序去重时用 LinkedHashSet（底层 LinkedHashMap）", data);
    }

    /**
     * TreeSet 自然排序与自定义 Comparator。
     */
    public ScenarioResult treesetSort() {
        ScenarioLog log = new ScenarioLog();

        Set<Integer> natural = new TreeSet<>();
        natural.add(5);
        natural.add(1);
        natural.add(9);
        natural.add(3);
        log.log("TreeSet 自然排序：%s", natural);

        Set<String> byLength = new TreeSet<>(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        byLength.add("banana");
        byLength.add("apple");
        byLength.add("kiwi");
        byLength.add("pear");
        log.log("TreeSet 自定义 Comparator（先按长度再按字典序）：%s", byLength);

        boolean exceptionCaught = false;
        try {
            Set<Object> mixed = new TreeSet<>();
            mixed.add("string");
            mixed.add(123);
        } catch (ClassCastException e) {
            exceptionCaught = true;
            log.log("往 TreeSet 放入不可比对象触发 ClassCastException");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("naturalSort", natural);
        data.put("customSort", byLength);
        data.put("exceptionCaught", exceptionCaught);
        return ScenarioResult.of(log, "TreeSet 基于红黑树，元素必须可比或提供 Comparator", data);
    }

    /**
     * equals/hashCode 契约。
     */
    public ScenarioResult equalsHashcodeContract() {
        ScenarioLog log = new ScenarioLog();

        // 违反契约：equals 相等但 hashCode 不等 -> HashSet 中去重失败/定位失败
        ViolationPerson vp = new ViolationPerson("Tom");
        Set<ViolationPerson> set = new HashSet<>();
        set.add(vp);
        boolean containsAfterPut = set.contains(vp);
        log.log("同一对象 contains=%s", containsAfterPut);

        ViolationPerson vp2 = new ViolationPerson("Tom");
        boolean containsAnother = set.contains(vp2);
        log.log("equals 相等但 hashCode 不等的对象 contains=%s（违反契约）", containsAnother);

        Map<String, Object> data = new HashMap<>();
        data.put("sameObjectContains", containsAfterPut);
        data.put("equalObjectContains", containsAnother);
        data.put("contractNote", "equals 相等则 hashCode 必须相等，否则 HashSet/HashMap 行为异常");
        return ScenarioResult.of(log, "equals 与 hashCode 必须同时重写且满足契约：等对象等 hashCode", data);
    }

    // ---------- 内部类 ----------

    private static class BadPerson {
        private final String name;
        private final int age;
        BadPerson(String name, int age) { this.name = name; this.age = age; }
        @Override public String toString() { return name + "(" + age + ")"; }
    }

    private static class GoodPerson {
        private final String name;
        private final int age;
        GoodPerson(String name, int age) { this.name = name; this.age = age; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoodPerson)) return false;
            GoodPerson that = (GoodPerson) o;
            return age == that.age && name.equals(that.name);
        }
        @Override public int hashCode() { return 31 * name.hashCode() + age; }
        @Override public String toString() { return name + "(" + age + ")"; }
    }

    private static class ViolationPerson {
        private final String name;
        ViolationPerson(String name) { this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ViolationPerson)) return false;
            return name.equals(((ViolationPerson) o).name);
        }
        @Override public int hashCode() {
            // 故意每次返回不同 hashCode，违反契约
            return (int) (System.nanoTime() % Integer.MAX_VALUE);
        }
    }
}

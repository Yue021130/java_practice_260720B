package com.example.cg.utils;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Collections / Arrays 工具类场景业务实现。
 *
 * 面试点：binarySearch 前必须 sort；synchronized 包装器迭代仍需同步；
 * unmodifiable 只是视图；Arrays.asList 底层是固定大小数组；shuffle 用种子可复现。
 */
@Service
public class UtilsScenarioService {

    /**
     * sort + binarySearch。
     */
    public ScenarioResult sortBinarysearch() {
        ScenarioLog log = new ScenarioLog();
        List<String> list = new ArrayList<>();
        list.add("banana");
        list.add("apple");
        list.add("cherry");
        list.add("date");
        log.log("原始 list=%s", list);

        list.sort(Comparator.naturalOrder());
        log.log("Collections.sort 自然排序后=%s", list);

        int idxApple = Collections.binarySearch(list, "apple");
        int idxMissing = Collections.binarySearch(list, "blueberry");
        log.log("binarySearch 'apple'=%d；binarySearch 'blueberry'=%d（负数表示不存在，插入点=-(idx+1)）", idxApple, idxMissing);

        // 多字段排序
        List<Person> people = new ArrayList<>();
        people.add(new Person("Alice", 30));
        people.add(new Person("Bob", 25));
        people.add(new Person("Alice", 25));
        people.sort(Comparator.comparing(Person::getName).thenComparingInt(Person::getAge));
        log.log("按 name 再按 age 排序=%s", people);

        Map<String, Object> data = new HashMap<>();
        data.put("sortedList", list);
        data.put("indexApple", idxApple);
        data.put("indexMissing", idxMissing);
        data.put("sortedPeople", people);
        return ScenarioResult.of(log, "binarySearch 要求 List 已排序；Comparator.comparing 支持链式多字段排序", data);
    }

    /**
     * synchronized 与 unmodifiable 包装器。
     */
    public ScenarioResult synchronizedUnmodifiable() {
        ScenarioLog log = new ScenarioLog();
        List<String> base = new ArrayList<>();
        base.add("a");
        base.add("b");

        List<String> sync = Collections.synchronizedList(new ArrayList<>(base));
        sync.add("c");
        log.log("synchronizedList 包装后 add 安全，size=%d", sync.size());

        List<String> unmod = Collections.unmodifiableList(new ArrayList<>(base));
        boolean caught = false;
        try {
            unmod.add("c");
        } catch (UnsupportedOperationException e) {
            caught = true;
            log.log("unmodifiableList add 抛出 UnsupportedOperationException");
        }

        // 原集合改变，视图也会变
        List<String> mutable = new ArrayList<>();
        mutable.add("x");
        List<String> view = Collections.unmodifiableList(mutable);
        mutable.add("y");
        log.log("原集合 mutable 新增 'y' 后，unmodifiable 视图也看到=%s", view);

        Map<String, Object> data = new HashMap<>();
        data.put("syncSize", sync.size());
        data.put("unmodifiableAddCaught", caught);
        data.put("viewAfterMutate", view);
        return ScenarioResult.of(log, "unmodifiable 只是视图，原集合改变视图也变；synchronized 迭代仍需外部同步", data);
    }

    /**
     * Arrays.asList 陷阱。
     */
    public ScenarioResult arraysAslistTrap() {
        ScenarioLog log = new ScenarioLog();
        List<String> list = Arrays.asList("a", "b", "c");
        log.log("Arrays.asList 得到 %s", list);

        list.set(0, "A"); // 可以修改元素
        log.log("set(0,'A') 后=%s（底层数组被修改）", list);

        boolean caught = false;
        try {
            list.add("d");
        } catch (UnsupportedOperationException e) {
            caught = true;
            log.log("add 抛出 UnsupportedOperationException：固定大小视图");
        }

        List<String> safe = new ArrayList<>(Arrays.asList("a", "b", "c"));
        safe.add("d");
        log.log("new ArrayList<>(Arrays.asList(...)) 后可 add=%s", safe);

        Map<String, Object> data = new HashMap<>();
        data.put("asList", list);
        data.put("addCaught", caught);
        data.put("safeList", safe);
        return ScenarioResult.of(log, "Arrays.asList 返回固定大小视图；想扩容用 new ArrayList<>(Arrays.asList(...))", data);
    }

    /**
     * Collections.shuffle。
     */
    public ScenarioResult shuffle() {
        ScenarioLog log = new ScenarioLog();
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) list.add(i);

        List<Integer> copy1 = new ArrayList<>(list);
        List<Integer> copy2 = new ArrayList<>(list);

        Collections.shuffle(copy1, new Random(12345));
        Collections.shuffle(copy2, new Random(12345));
        log.log("相同 Random 种子 shuffle 两次：%s 与 %s", copy1, copy2);

        Map<String, Object> data = new HashMap<>();
        data.put("sameSeedResult1", copy1);
        data.put("sameSeedResult2", copy2);
        data.put("sameSeedEqual", copy1.equals(copy2));
        return ScenarioResult.of(log, "shuffle 用相同 Random 种子可复现同一乱序结果", data);
    }

    // ---------- 内部类 ----------

    private static class Person {
        private final String name;
        private final int age;
        Person(String name, int age) { this.name = name; this.age = age; }
        public String getName() { return name; }
        public int getAge() { return age; }
        @Override public String toString() { return name + "(" + age + ")"; }
    }
}

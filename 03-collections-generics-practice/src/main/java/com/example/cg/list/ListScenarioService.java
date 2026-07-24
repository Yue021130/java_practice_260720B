package com.example.cg.list;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * List 场景业务实现。
 *
 * 面试点：ArrayList 底层是 Object 数组，随机访问 O(1)、中间插入/删除 O(n)；
 * LinkedList 双向链表，随机访问 O(n)、头尾操作 O(1)；subList 是视图；Iterator 是 fail-fast。
 */
@Service
public class ListScenarioService {

    /**
     * ArrayList vs LinkedList 性能对比。
     */
    public ScenarioResult arraylistVsLinkedlist(int size) {
        ScenarioLog log = new ScenarioLog();
        Map<String, Object> data = new HashMap<>();

        // 随机访问对比
        List<Integer> arrayList = new ArrayList<>(size);
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        long t0 = System.nanoTime();
        long sumArray = 0;
        for (int i = 0; i < size; i += 10) sumArray += arrayList.get(i);
        long arrayRandom = (System.nanoTime() - t0) / 1_000_000;
        log.log("ArrayList 随机访问 %d 条，耗时 %dms，sum=%d", size / 10, arrayRandom, sumArray);

        long t1 = System.nanoTime();
        long sumLinked = 0;
        for (int i = 0; i < size; i += 10) sumLinked += linkedList.get(i);
        long linkedRandom = (System.nanoTime() - t1) / 1_000_000;
        log.log("LinkedList 随机访问 %d 条，耗时 %dms，sum=%d", size / 10, linkedRandom, sumLinked);

        // 中间插入对比（只插 1000 条，避免太久）
        int insertCount = Math.min(1000, size / 10);
        List<Integer> alInsert = new ArrayList<>(arrayList);
        long t2 = System.nanoTime();
        for (int i = 0; i < insertCount; i++) alInsert.add(alInsert.size() / 2, -1);
        long arrayInsert = (System.nanoTime() - t2) / 1_000_000;
        log.log("ArrayList 中间插入 %d 次，耗时 %dms", insertCount, arrayInsert);

        List<Integer> llInsert = new LinkedList<>(linkedList);
        long t3 = System.nanoTime();
        for (int i = 0; i < insertCount; i++) llInsert.add(llInsert.size() / 2, -1);
        long linkedInsert = (System.nanoTime() - t3) / 1_000_000;
        log.log("LinkedList 中间插入 %d 次，耗时 %dms", insertCount, linkedInsert);

        data.put("size", size);
        data.put("arrayRandomAccessMs", arrayRandom);
        data.put("linkedRandomAccessMs", linkedRandom);
        data.put("arrayMiddleInsertMs", arrayInsert);
        data.put("linkedMiddleInsertMs", linkedInsert);
        data.put("winnerRandom", arrayRandom <= linkedRandom ? "ArrayList" : "LinkedList");
        data.put("winnerInsert", linkedInsert <= arrayInsert ? "LinkedList" : "ArrayList");

        String summary = String.format("随机访问 ArrayList 更快，中间插入 LinkedList 通常更快（size=%d）", size);
        return ScenarioResult.of(log, summary, data);
    }

    /**
     * subList 是视图而非拷贝。
     */
    public ScenarioResult sublistTrap() {
        ScenarioLog log = new ScenarioLog();
        List<String> origin = new ArrayList<>();
        origin.add("a");
        origin.add("b");
        origin.add("c");
        origin.add("d");
        origin.add("e");

        List<String> sub = origin.subList(1, 4); // b,c,d
        log.log("原 list=%s，subList(1,4)=%s", origin, sub);

        sub.set(0, "B");
        log.log("修改 sub[0]='B' 后，sub=%s，原 list=%s（视图联动）", sub, origin);

        sub.add("x");
        log.log("sub.add('x') 后，sub=%s，原 list=%s（长度一起变）", sub, origin);

        // 正确用法：需要独立拷贝时 new ArrayList<>(sub)
        List<String> copy = new ArrayList<>(sub);
        copy.set(0, "copy-B");
        log.log("new ArrayList<>(sub) 得到独立拷贝，修改后 copy=%s，原 list=%s（互不影响）", copy, origin);

        Map<String, Object> data = new HashMap<>();
        data.put("originAfterSubModify", origin);
        data.put("subAfterAdd", sub);
        data.put("copy", copy);
        return ScenarioResult.of(log, "subList 是原 List 的视图；想隔离必须 new ArrayList<>(subList)", data);
    }

    /**
     * ListIterator 双向遍历并修改。
     */
    public ScenarioResult listIterator() {
        ScenarioLog log = new ScenarioLog();
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        list.add("D");

        ListIterator<String> it = list.listIterator();
        while (it.hasNext()) {
            String s = it.next();
            if ("B".equals(s)) it.set("B-modified");
            log.log("正向遍历：%s", s);
        }
        log.log("正向遍历 + set 后 list=%s", list);

        while (it.hasPrevious()) {
            String s = it.previous();
            if ("D".equals(s)) it.add("D-after");
            log.log("反向遍历：%s", s);
        }
        log.log("反向遍历 + add 后 list=%s", list);

        Map<String, Object> data = new HashMap<>();
        data.put("finalList", list);
        return ScenarioResult.of(log, "ListIterator 支持双向遍历、set、add；普通 Iterator 只能单向 remove", data);
    }

    /**
     * fail-fast 迭代器。
     */
    public ScenarioResult iteratorFailfast() {
        ScenarioLog log = new ScenarioLog();
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        boolean caught = false;
        try {
            for (String s : list) {
                if ("b".equals(s)) list.remove(s); // 结构性修改
            }
        } catch (ConcurrentModificationException e) {
            caught = true;
            log.log("增强 for 中 remove 触发 ConcurrentModificationException");
        }

        // 正确做法：用 Iterator.remove()
        List<String> list2 = new ArrayList<>();
        list2.add("a");
        list2.add("b");
        list2.add("c");
        java.util.Iterator<String> it = list2.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if ("b".equals(s)) it.remove();
        }
        log.log("使用 Iterator.remove() 安全删除后 list=%s", list2);

        Map<String, Object> data = new HashMap<>();
        data.put("caughtException", caught);
        data.put("safeRemoveResult", list2);
        return ScenarioResult.of(log, "fail-fast：迭代中直接结构性修改会抛异常；应使用 Iterator.remove()", data);
    }
}

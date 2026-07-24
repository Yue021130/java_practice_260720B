package com.example.cg.mapordered;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

/**
 * 有序/排序 Map 场景业务实现。
 *
 * 面试点：LinkedHashMap accessOrder 实现 LRU；TreeMap 红黑树排序；WeakHashMap key 是弱引用。
 */
@Service
public class MapOrderedScenarioService {

    /**
     * LinkedHashMap 实现 LRU 缓存。
     */
    public ScenarioResult linkedhashmapLru(int capacity) {
        ScenarioLog log = new ScenarioLog();

        LinkedHashMap<String, String> lru = new LinkedHashMap<String, String>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                boolean remove = size() > capacity;
                if (remove) {
                    log.log("缓存超限，淘汰最久未使用：%s=%s", eldest.getKey(), eldest.getValue());
                }
                return remove;
            }
        };

        for (int i = 1; i <= capacity + 2; i++) {
            lru.put("k" + i, "v" + i);
            log.log("put k%d，缓存=%s", i, lru.keySet());
        }

        lru.get("k3"); // 访问 k3，使其变新
        log.log("访问 k3 后，缓存=%s", lru.keySet());

        lru.put("k-new", "v-new");
        log.log("put k-new 后，缓存=%s", lru.keySet());

        Map<String, Object> data = new HashMap<>();
        data.put("capacity", capacity);
        data.put("finalKeys", lru.keySet());
        return ScenarioResult.of(log, "LinkedHashMap accessOrder=true + removeEldestEntry 实现 LRU", data);
    }

    /**
     * TreeMap 排序与范围查询。
     */
    public ScenarioResult treemapSort() {
        ScenarioLog log = new ScenarioLog();

        TreeMap<Integer, String> scores = new TreeMap<>();
        scores.put(85, "Alice");
        scores.put(92, "Bob");
        scores.put(78, "Carol");
        scores.put(95, "Dave");
        log.log("TreeMap 按键自然排序：%s", scores);

        Map.Entry<Integer, String> first = scores.firstEntry();
        Map.Entry<Integer, String> last = scores.lastEntry();
        Map<Integer, String> above80 = scores.subMap(80, true, 100, true);
        log.log("first=%s，last=%s，≥80 分区=%s", first, last, above80);

        TreeMap<String, Integer> byNameLen = new TreeMap<>(Comparator.comparingInt(String::length));
        byNameLen.put("banana", 1);
        byNameLen.put("apple", 2);
        byNameLen.put("kiwi", 3);
        log.log("自定义 Comparator（按字符串长度）：%s", byNameLen);

        Map<String, Object> data = new HashMap<>();
        data.put("naturalSort", scores);
        data.put("first", first);
        data.put("last", last);
        data.put("subMap", above80);
        data.put("customSort", byNameLen);
        return ScenarioResult.of(log, "TreeMap 基于红黑树，支持自然/自定义排序与范围查询", data);
    }

    /**
     * WeakHashMap：key 是弱引用，GC 后自动清理。
     */
    public ScenarioResult weakhashmapCache() {
        ScenarioLog log = new ScenarioLog();
        WeakHashMap<Object, String> weak = new WeakHashMap<>();

        Object key1 = new Object();
        Object key2 = new Object();
        weak.put(key1, "value1");
        weak.put(key2, "value2");
        log.log("初始 size=%d", weak.size());

        key1 = null; // 断开强引用
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.log("key1 强引用断开并 GC 后，size=%d", weak.size());

        Map<String, Object> data = new HashMap<>();
        data.put("initialSize", 2);
        data.put("sizeAfterGc", weak.size());
        return ScenarioResult.of(log, "WeakHashMap 的 key 是弱引用，无强引用时 GC 会回收对应 Entry", data);
    }
}

package com.example.cg.maphash;

import com.example.cg.common.ScenarioLog;
import com.example.cg.common.ScenarioResult;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashMap 原理场景业务实现。
 *
 * 面试点：hash() 扰动、(n-1)&hash 定位、容量为 2 的幂、负载因子 0.75、
 * 链表长度≥8 且容量≥64 树化、key 必须保证 hashCode 不变、CHM 不支持 null。
 */
@Service
public class MapHashScenarioService {

    /**
     * HashMap 内部机制：扰动、容量、扩容。
     */
    public ScenarioResult hashmapInternals() {
        ScenarioLog log = new ScenarioLog();
        Map<String, Integer> map = new HashMap<>(4, 0.75f);
        log.log("初始容量=%d，负载因子=0.75", 4);

        // 用反射观察 table 长度（容量）变化
        int prevCapacity = 0;
        for (int i = 0; i < 17; i++) {
            map.put("key-" + i, i);
            int cap = tableCapacity(map);
            if (i == 0 || cap != prevCapacity) {
                log.log("插入 key-%d 后，table 容量变为 %d", i, cap);
                prevCapacity = cap;
            }
        }

        // 演示 hash 扰动：hashCode 高位参与运算
        int h = "hello".hashCode();
        int disturbed = h ^ (h >>> 16);
        log.log("\"hello\" hashCode=%d，扰动后=%d（高 16 位与低 16 位异或）", h, disturbed);

        Map<String, Object> data = new HashMap<>();
        data.put("finalSize", map.size());
        data.put("finalCapacity", tableCapacity(map));
        data.put("hashCodeHello", h);
        data.put("disturbedHash", disturbed);
        return ScenarioResult.of(log, "HashMap 容量始终为 2 的幂，hash 经扰动后 &(n-1) 定位桶", data);
    }

    private int tableCapacity(Map<?, ?> map) {
        try {
            Field tableField = HashMap.class.getDeclaredField("table");
            tableField.setAccessible(true);
            Object[] table = (Object[]) tableField.get(map);
            return table == null ? 0 : table.length;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * key 被修改后丢失。
     */
    public ScenarioResult keyMutation() {
        ScenarioLog log = new ScenarioLog();
        Map<MutableKey, String> map = new HashMap<>();
        MutableKey key = new MutableKey("Tom");
        map.put(key, "value-of-Tom");
        log.log("put 后 map.size=%d，get=%s", map.size(), map.get(key));

        key.name = "Jerry"; // 修改 key 字段
        String afterMutate = map.get(key);
        log.log("修改 key.name='Jerry' 后，get 同一个 key=%s（找不到或找错桶）", afterMutate);

        boolean stillContains = map.containsKey(key);
        log.log("containsKey=%s", stillContains);

        Map<String, Object> data = new HashMap<>();
        data.put("beforeMutateValue", "value-of-Tom");
        data.put("afterMutateValue", afterMutate);
        data.put("containsAfterMutate", stillContains);
        return ScenarioResult.of(log, "HashMap 的 key 必须不可变或保证 hashCode 不变，否则 get 失效", data);
    }

    /**
     * HashMap vs Hashtable vs ConcurrentHashMap。
     */
    public ScenarioResult mapCompare() {
        ScenarioLog log = new ScenarioLog();
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put(null, "null-key");
        hashMap.put("null-value", null);
        log.log("HashMap 允许 null key 和 null value");

        Map<String, String> hashtable = new Hashtable<>();
        boolean hashtableNullKeyOk = false;
        boolean hashtableNullValueOk = false;
        try {
            hashtable.put(null, "x");
            hashtableNullKeyOk = true;
        } catch (NullPointerException e) {
            log.log("Hashtable 不允许 null key");
        }
        try {
            hashtable.put("x", null);
            hashtableNullValueOk = true;
        } catch (NullPointerException e) {
            log.log("Hashtable 不允许 null value");
        }

        Map<String, String> chm = new ConcurrentHashMap<>();
        boolean chmNullKeyOk = false;
        boolean chmNullValueOk = false;
        try {
            chm.put(null, "x");
            chmNullKeyOk = true;
        } catch (NullPointerException e) {
            log.log("ConcurrentHashMap 不允许 null key");
        }
        try {
            chm.put("x", null);
            chmNullValueOk = true;
        } catch (NullPointerException e) {
            log.log("ConcurrentHashMap 不允许 null value");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("hashMapNullKey", true);
        data.put("hashMapNullValue", true);
        data.put("hashtableNullKey", hashtableNullKeyOk);
        data.put("hashtableNullValue", hashtableNullValueOk);
        data.put("chmNullKey", chmNullKeyOk);
        data.put("chmNullValue", chmNullValueOk);
        data.put("threadSafe", "Hashtable/CHM 线程安全，HashMap 不线程安全");
        return ScenarioResult.of(log, "HashMap 允许 null；Hashtable/CHM 线程安全但都不允许 null key/value", data);
    }

    // ---------- 内部类 ----------

    private static class MutableKey {
        String name;
        MutableKey(String name) { this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MutableKey)) return false;
            return name.equals(((MutableKey) o).name);
        }
        @Override public int hashCode() { return name.hashCode(); }
        @Override public String toString() { return name; }
    }
}

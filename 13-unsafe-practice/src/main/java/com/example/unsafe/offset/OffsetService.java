package com.example.unsafe.offset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 05. 字段偏移与对象布局：objectFieldOffset / arrayBaseOffset / arrayIndexScale。
 *
 * <p>JUC 的 AtomicInteger 就是这么干的：先反射拿到 value 字段的偏移量存起来，
 * 之后所有读写都走 Unsafe.getIntVolatile(对象, 偏移) —— 不再碰反射，性能接近裸字段访问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OffsetService {

    private final Unsafe unsafe;

    /** 实验对象：多种类型的字段，观察它们的偏移量 */
    public static class LayoutDemo {
        private long aLong = 100L;      // 8 字节
        private int anInt = 10;         // 4 字节
        private boolean aBool = true;   // 1 字节
        private byte aByte = 1;         // 1 字节
        private short aShort = 2;       // 2 字节
        private Object ref = "ref";     // 引用（开启压缩指针时 4 字节）
        private double aDouble = 1.5;   // 8 字节
    }

    /**
     * 打印各字段偏移量，并推断对象头大小与字段排列规律。
     */
    public Map<String, Object> fields() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        LayoutDemo demo = new LayoutDemo();
        long minOffset = Long.MAX_VALUE;
        for (Field f : LayoutDemo.class.getDeclaredFields()) {
            long offset = unsafe.objectFieldOffset(f);
            minOffset = Math.min(minOffset, offset);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("field", f.getName());
            row.put("type", f.getType().getSimpleName());
            row.put("offset", offset);
            row.put("hex", "0x" + Long.toHexString(offset));
            rows.add(row);
        }
        result.put("rows", rows);
        result.put("firstFieldOffset", minOffset);
        result.put("estimatedObjectHeader", minOffset);
        result.put("headerNote", "第一个字段的偏移 ≈ 对象头大小。JDK 17 默认开启压缩指针：mark word(8B) + klass(4B) = 12B，"
                + "所以第一个字段一般落在偏移 12。");
        result.put("layoutNote", "HotSpot 会按 规则打包字段：long/double 8 字节对齐，int 4 字节对齐，"
                + "引用在压缩指针下占 4 字节。所以声明顺序 ≠ 内存顺序，中间有填充。");
        return result;
    }

    /**
     * 打破封装：用偏移量直接读写 private 字段，绕过 getter/setter。
     */
    public Map<String, Object> directWrite() throws NoSuchFieldException {
        SecretBox box = new SecretBox();
        long offset = unsafe.objectFieldOffset(SecretBox.class.getDeclaredField("secret"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("before", box.getSecret());            // 42
        result.put("secretFieldOffset", offset);
        result.put("readViaUnsafe", unsafe.getInt(box, offset)); // 不走 getter 读

        unsafe.putInt(box, offset, 999);                   // 不走 setter 写
        result.put("afterUnsafeWrite", box.getSecret());   // 999
        result.put("afterReadViaUnsafe", unsafe.getInt(box, offset));

        result.put("tip", "AtomicInteger / AtomicLong 内部就是如此读写 value 字段；"
                + "它也意味着私有性只是“语言层面的纸老虎”，Unsafe 面前没有封装。");
        return result;
    }

    /**
     * 数组定位：arrayBaseOffset + arrayIndexScale，按地址直接索引数组元素。
     */
    public Map<String, Object> array() {
        int[] arr = new int[]{3, 6, 9, 12};
        int base = unsafe.arrayBaseOffset(int[].class);
        int scale = unsafe.arrayIndexScale(int[].class); // int 数组元素间距 = 4

        List<Map<String, Object>> reads = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            long elemAddr = base + (long) i * scale;
            int value = unsafe.getInt(arr, elemAddr); // arr[0] = getInt(arr, base)
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", i);
            row.put("elementAddress", base + (long) i * scale);
            row.put("valueViaUnsafe", value);
            reads.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("array", "new int[]{3, 6, 9, 12}");
        result.put("arrayBaseOffset", base);
        result.put("arrayIndexScale", scale);
        result.put("formula", "第 i 个元素地址 = arrayBaseOffset + i * arrayIndexScale");
        result.put("reads", reads);
        result.put("tip", "ConcurrentHashMap 的 Node 数组、JDK 内部的 Unsafe 数组遍历都用这套公式直接寻址，"
                + "避免一次次的数组边界检查。");
        return result;
    }

    /**
     * 对象内存布局示意图（文字版）。
     */
    public Map<String, Object> layout() {
        List<String> lines = new ArrayList<>();
        lines.add("对象布局 = 对象头 + 实例字段区 + 对齐填充（对象大小按 8 字节对齐）");
        lines.add("");
        lines.add("┌──────────────────────────────┐");
        lines.add("│  Mark Word（锁状态/GC 年龄/hash） 8 字节  │");
        lines.add("├──────────────────────────────┤");
        lines.add("│  Klass Pointer（类元数据指针）    4 字节  │  ← 压缩指针默认开启（-XX:+UseCompressedClassPointers）");
        lines.add("├──────────────────────────────┤");
        lines.add("│  long 字段   8 字节                │");
        lines.add("│  int 字段    4 字节                │");
        lines.add("│  boolean+byte+short 按规则打包填充    │");
        lines.add("│  引用字段    4 字节（压缩）           │");
        lines.add("├──────────────────────────────┤");
        lines.add("│  对齐填充（padding）               │");
        lines.add("└──────────────────────────────┘");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("diagram", lines);
        result.put("whyImportant", "① 理解 synchronized 升级（锁信息存在 Mark Word）；② 理解字段偏移与对象大小（缓存行/伪共享）；"
                + "③ 理解为什么压缩指针能省内存。JDK 17 里可用 -XX:+UseCompressedOops / -XX:-UseCompressedOops 开关对比。");
        return result;
    }

    /** 私有字段实验对象 */
    public static class SecretBox {
        private int secret = 42;

        public int getSecret() {
            return secret;
        }
    }
}

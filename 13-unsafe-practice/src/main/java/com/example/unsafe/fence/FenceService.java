package com.example.unsafe.fence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 07. 内存屏障：loadFence / storeFence / fullFence，volatile 的底层实现。
 *
 * <p>volatile 读写 → JVM 编译后插入内存屏障 → 最终落到 CPU 指令
 * （x86 上 StoreStore/StoreLoad 用 lock 前缀指令实现）。这里用普通字段 + 屏障
 * 手写一个“volatile 效果”，把底层屏障的真实用法亮出来。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FenceService {

    private final Unsafe unsafe;

    /** 用普通（非 volatile）字段演示屏障 */
    public static class FenceFlag {
        int data;   // 生产的数据
        int ready;  // 就绪标记：1 = 已就绪
    }

    /**
     * 演示：普通字段 + 屏障实现跨线程“先写数据、再置就绪，读方先看就绪再看数据”的保序。
     */
    public Map<String, Object> demo() throws NoSuchFieldException {
        FenceFlag flag = new FenceFlag();
        long dataOffset = unsafe.objectFieldOffset(FenceFlag.class.getDeclaredField("data"));
        long readyOffset = unsafe.objectFieldOffset(FenceFlag.class.getDeclaredField("ready"));

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> code = new ArrayList<>();
        code.add("// 生产线程（写方）—— 相当于 volatile 写");
        code.add("unsafe.putInt(flag, dataOffset, 42);   // 1. 先写数据");
        code.add("unsafe.storeFence();                   // 2. StoreStore 屏障：禁止上面写重排到下面写之后");
        code.add("unsafe.putInt(flag, readyOffset, 1);   // 3. 再置就绪标记");
        code.add("");
        code.add("// 消费线程（读方）—— 相当于 volatile 读");
        code.add("int r = unsafe.getInt(flag, readyOffset);");
        code.add("unsafe.loadFence();                    // 4. LoadLoad 屏障：先读就绪，再读数据");
        code.add("if (r == 1) { int d = unsafe.getInt(flag, dataOffset); }");

        // 真跑一遍：单线程模拟两段逻辑并顺序执行，验证屏障调用本身正常
        unsafe.putInt(flag, dataOffset, 42);
        unsafe.storeFence();
        unsafe.putInt(flag, readyOffset, 1);
        unsafe.loadFence();
        int ready = unsafe.getInt(flag, readyOffset);
        int data = unsafe.getInt(flag, dataOffset);
        unsafe.fullFence(); // 全屏障

        result.put("code", code);
        result.put("executed", "read: ready=" + ready + ", data=" + data);
        result.put("fenceTypes", new String[]{
                "loadFence()   — 禁止该点之后的读操作重排到它之前（LoadLoad + LoadStore）",
                "storeFence()  — 禁止该点之前的写操作重排到它之后（StoreStore）",
                "fullFence()   — 全屏障，读写都不越过（StoreLoad，最贵）"
        });
        result.put("tip", "为什么加屏障：CPU/编译器可能重排指令。没有 StoreStore 屏障，写方可能“先置就绪、后写数据”，"
                + "读方就会看到 ready=1 但 data 还是旧值。volatile 在源码层帮你把这些屏障都插好了。");
        return result;
    }

    /**
     * JMM 与 volatile 底层八股。
     */
    public Map<String, Object> explain() {
        List<Map<String, String>> items = new ArrayList<>();
        items.add(map("volatile 两层语义", "① 可见性：写 volatile 立即对其他线程可见；② 有序性：禁止指令重排序（插入屏障）"));
        items.add(map("不保证什么", "不保证原子性：i++ 这类读-改-写还是线程不安全的，需要 CAS/锁"));
        items.add(map("JMM 8 种内存操作", "read/load/use/assign/store/write（6 种基本）+ lock/unlock（2 种锁操作），对应用 4 条规则约束执行顺序"));
        items.add(map("4 条 Happens-Before 规则", "① 程序顺序规则；② 锁规则（解锁 Happens-Before 加锁）；③ volatile 规则（volatile 写 HB volatile 读）；④ 传递性"));
        items.add(map("x86 上的实现", "普通 StoreLoad 才真正需要屏障，x86 用 lock 前缀指令（如 CMPXCHG）实现；其余屏障在 x86 下基本免费"));
        items.add(map("与 Unsafe 的关系", "JVM 内部就是用 loadFence/storeFence/fullFence 这几个方法去实现 volatile 与锁的语义"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("tip", "面试题：volatile 能保证原子性吗？能保证有序性和可见性吗？——前者否，后两者是。");
        return result;
    }

    private Map<String, String> map(String k, String v) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("item", k);
        m.put("detail", v);
        return m;
    }
}

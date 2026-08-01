package com.example.tl.advance;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ThreadLocal 进阶：内存泄漏与最佳实践。
 */
@Service
public class AdvanceService {

    /**
     * 内存泄漏原理分析。
     */
    public Map<String, Object> leakAnalysisDemo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("structure", "Thread -> ThreadLocalMap -> Entry[] -> Entry { WeakReference<ThreadLocal<?>> key, Object value }");
        result.put("keyReference", "弱引用：ThreadLocal 实例没有强引用时会被 GC 回收，key 变成 null");
        result.put("valueReference", "强引用：即使 key 为 null，value 仍被当前线程强引用，无法回收");
        result.put("danger", "线程池中的线程长期存活，若任务 set 后未 remove，value 会堆积导致 OOM");
        result.put("defense", "1) 用完 remove；2) 避免大对象；3) static final 但注意生命周期；4) 线程池任务 try-finally remove");
        result.put("note", "ThreadLocalMap 的 set/get/remove 过程中会触发 expungeStaleEntry 清理 key 为 null 的条目，但这不是实时清理");
        return result;
    }

    /**
     * 最佳实践：static final + try-finally remove。
     */
    public Map<String, Object> bestPracticeDemo() {
        Map<String, Object> result = new HashMap<>();
        List<String> practices = new ArrayList<>();
        practices.add("声明为 private static final，避免被外部误清空");
        practices.add("在 finally 块中调用 remove()，确保即使抛异常也清理");
        practices.add("线程池任务中必须 remove，否则串号 + 内存泄漏");
        practices.add("value 不要放超大对象，降低泄漏风险");
        practices.add("优先使用 JDK 8+ 的 ThreadLocal.withInitial 提供默认值");
        practices.add("跨线程池传递上下文优先使用 Alibaba TTL");

        ThreadLocal<String> local = ThreadLocal.withInitial(() -> "default");
        String lifecycle = "";
        try {
            local.set("business-value");
            lifecycle = "set -> " + local.get();
        } finally {
            local.remove();
            lifecycle += " -> remove -> " + local.get();
        }

        result.put("practices", practices);
        result.put("lifecycle", lifecycle);
        result.put("note", "try-finally 是 ThreadLocal 使用的黄金法则");
        return result;
    }
}

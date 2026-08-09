package com.example.unsafe.intro;

import com.example.unsafe.common.UnsafeBizException;
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
 * 01. 初识 Unsafe：如何获取实例、六大能力地图、为什么叫“魔法类”、为什么官方禁用。
 *
 * <p>获取 Unsafe 的唯一“正规”入口 {@link Unsafe#getUnsafe()} 只对 Bootstrap 类加载器
 * 加载的类开放（JDK 内部类），普通应用直接调用必然抛 {@code SecurityException}——
 * 这里先把“正规入口为什么被堵死”跑给你看，再演示反射破解。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntroService {

    private final Unsafe unsafe;

    /**
     * 能力总览：验证 Unsafe 实例可用，并输出六大能力地图。
     */
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        // 注意：不能把 Unsafe 实例本身塞进 Map——Jackson 序列化无属性 bean 会抛异常，这里放 toString
        result.put("unsafeInstance", String.valueOf(unsafe));
        result.put("unsafeClassLoader", unsafe.getClass().getClassLoader() == null
                ? "Bootstrap ClassLoader（null）——JDK 内部加载，这就是普通类拿不到它的原因"
                : unsafe.getClass().getClassLoader());
        result.put("jvmVersion", System.getProperty("java.version"));
        result.put("jvmVendor", System.getProperty("java.vm.name"));

        // 六大能力地图：每个能力对应后面的实验章节
        List<Map<String, String>> capabilities = new ArrayList<>();
        capabilities.add(map("内存操作", "allocateMemory / freeMemory / putInt / getInt / setMemory / copyMemory",
                "直接分配并读写堆外内存，不受 GC 管理（见 02 memory）"));
        capabilities.add(map("对象实例化", "allocateInstance(Class)",
                "不调用任何构造器创建对象，字段保持默认值（见 03 instance）"));
        capabilities.add(map("CAS 原子操作", "compareAndSwapInt / compareAndSwapLong / compareAndSwapObject",
                "硬件级原子比较并交换，JUC 原子类的底层（见 04 cas）"));
        capabilities.add(map("字段偏移", "objectFieldOffset / arrayBaseOffset / arrayIndexScale",
                "定位字段在对象/数组内的内存偏移，打破封装读写（见 05 offset）"));
        capabilities.add(map("线程阻塞", "park / unpark",
                "阻塞与唤醒线程，LockSupport 的底层（见 06 park）"));
        capabilities.add(map("内存屏障", "loadFence / storeFence / fullFence",
                "控制指令重排序与可见性，volatile 的底层（见 07 fence）"));
        result.put("capabilities", capabilities);

        result.put("tip", "sun.misc.Unsafe 是 JDK 内部用于并发、序列化、堆外内存的底层工具，"
                + "本专题把它包装成 Spring Bean 后逐项实验。");
        return result;
    }

    /**
     * 演示 getUnsafe() 正规入口被堵死：普通应用调用必抛 SecurityException。
     */
    public Map<String, Object> getUnsafeDemo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", "Unsafe.getUnsafe();");
        try {
            Unsafe u = Unsafe.getUnsafe();
            result.put("result", "意外成功：" + u);
        } catch (SecurityException e) {
            result.put("result", "抛异常：SecurityException - " + e.getMessage());
            result.put("why", "getUnsafe() 源码里先检查类加载器：只有 BootstrapClassLoader（null）加载的类才放行。"
                    + "我们的类由 AppClassLoader 加载，直接调用必然被拒，这是 JDK 的自我保护。");
            result.put("workaround", "反射读取内部字段 theUnsafe（见 UnsafeConfig），这是所有框架的通行做法。");
        }
        return result;
    }

    /**
     * 为什么叫“魔法类”、为什么官方不建议使用。
     */
    public Map<String, Object> why() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("whyMagic", "因为它绕过了 Java 语言层面的所有安全检查，像魔法一样直接操作底层："
                + "不 new 也能造对象、能读写任意内存、能修改对象私有字段、能直接操作线程。");
        result.put("whyForbidden", "① 越界访问内存会直接 SIGSEGV 让 JVM 崩溃，不抛任何 Java 异常；"
                + "② 绕过构造器/破坏封装，对象可能处于非法状态；"
                + "③ 依赖 JDK 内部实现，跨版本不保证兼容，移植性差；"
                + "④ 官方只面向 JDK 内部，方法无契约、可能在未来版本被替换或删除。");
        result.put("standard", "生产代码绝不直接使用；需要这些能力优先用 JUC / VarHandle / 官方 API，"
                + "或者用封装好的第三方库（Netty / Kryo 等）。");
        return result;
    }

    private Map<String, String> map(String name, String methods, String scene) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("methods", methods);
        m.put("scene", scene);
        return m;
    }
}

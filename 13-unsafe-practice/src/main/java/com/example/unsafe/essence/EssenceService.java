package com.example.unsafe.essence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 08. 危险与本质：为什么说 Unsafe 是“双刃剑”、它为什么存在、JDK 的演变与未来。
 *
 * <p>注意：本模块只展示风险代码的“文本形态”，刻意不真正执行
 * （越界访问会让 JVM 直接 SIGSEGV 崩溃，无法被 try-catch 捕获）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EssenceService {

    /**
     * 四大风险，每个都给出“代码长什么样”与“后果是什么”。
     */
    public Map<String, Object> risks() {
        List<Map<String, Object>> risks = new ArrayList<>();
        risks.add(risk("① 越界访问内存 → JVM 崩溃",
                "unsafe.putInt(0xFFFFFFFFL, 1);  // 写一个非法地址",
                "不抛 Java 异常，直接 SIGSEGV 段错误让整个 JVM 死掉，连 finally / 日志都来不及。"
                        + "这是 Unsafe 最危险的地方，也是它名字的由来之一。"));
        risks.add(risk("② 堆外内存泄漏",
                "long p = unsafe.allocateMemory(1GB);  // 忘掉 freeMemory(p)",
                "堆外内存不归 GC 管，漏一次就永久少一块；长时间运行被系统 OOM Killer 干掉，"
                        + "监控还看不到（堆指标正常）。Netty 配置不当导致堆外泄漏是经典生产事故。"));
        risks.add(risk("③ 破坏封装 → 对象非法状态",
                "unsafe.putInt(obj, offset, -999);  // 绕过 setter/校验直接写字段",
                "不变量被破坏：余额可为负、final 字段可为 0、单例变多例。业务正确性完全失控。"));
        risks.add(risk("④ 不可移植",
                "依赖 sun.misc 内部实现与对象布局细节",
                "JDK 版本升级字段偏移可能变、方法可能删（JDK 9 起官方就说不保证兼容），代码换个 JDK 就崩。"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("risks", risks);
        result.put("rule", "铁律：生产代码不直接 new 操作 Unsafe；需要底层能力走官方 API（VarHandle / JUC）或封装好的第三方库。");
        return result;
    }

    /**
     * 本质：Unsafe 到底是个什么存在。
     */
    public Map<String, Object> essence() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("definition", "sun.misc.Unsafe 是 JVM 暴露给 JDK 内部使用的一层“后门”："
                + "把 Java 语言层面藏起来的底层能力（内存、对象、线程、屏障）直接裸露出来。");
        result.put("whyExists", "① JDK 自己需要：并发（JUC）、序列化、NIO 都需要堆外内存与 CAS，"
                + "不能每次都用 native 方法 + 反射，效率太低；"
                + "② 有些能力 Java 语言层面根本表达不了：直接内存操作、绕过构造器、精确唤醒线程。");
        result.put("whyItIs", "它绕过了三道 Java 安全防线：类型安全（可写任意内存）、"
                + "封装（可改 private/final）、内存管理（堆外不归 GC）。"
                + "所以它叫 Unsafe——不安全，但能力巨大。");
        result.put("metaphor", "如果把 Java 比作一栋有门禁的写字楼（语法糖、自动内存管理、安全检查），"
                + "Unsafe 就是物业留的一把万能钥匙：关键时刻能开锁修东西，但也意味着谁拿了它都能进任何房间。");
        result.put("position", "学习它的意义：看懂 JUC / Netty / 各种框架“为什么快”的底层原理，"
                + "而不是鼓励你在业务代码里用它。");
        return result;
    }

    /**
     * JDK 版本演变与官方替代方案。
     */
    public Map<String, Object> evolution() {
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row("JDK 1.x~8", "sun.misc.Unsafe 独霸天下：JUC、序列化全靠它，普通类也能反射拿到实例"));
        rows.add(row("JDK 9+ 模块化", "Unsafe 收进 jdk.unsupported 模块（默认导出）；官方态度明确：内部用、不建议外部用"));
        rows.add(row("JDK 9（JEP 193）", "引入 VarHandle：类型安全、可移植的“官方版 Unsafe”，支持 CAS、屏障、偏移量操作"));
        rows.add(row("JDK 16/17+", "越来越多框架转向 VarHandle（ConcurrentHashMap 在 JDK 9+ 已用 VarHandle 实现）；"
                + "Unsafe 新方法基本冻结，官方表示长期目标是内部化/替代"));
        rows.add(row("未来", "JDK 24 起部分 Unsafe 能力仍在使用但不断被 VarHandle / 新 API 取代；"
                + "新代码请直接学 VarHandle，它是官方给的方向"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("tip", "面试题：Unsafe 会被移除吗？——短期不会（JDK 内部还在用），但官方口径是“不该用”，新特性都走 VarHandle。");
        result.put("varHandleSnippet", new String[]{
                "// 官方替代：VarHandle（JDK 9+）",
                "VarHandle VH = MethodHandles.lookup().findVarHandle(Cls.class, \"x\", int.class);",
                "VH.get(obj); VH.set(obj, 1); VH.compareAndSet(obj, 0, 1); // 与 Unsafe CAS 同语义，但类型安全"
        });
        return result;
    }

    /**
     * 真实世界谁在用 Unsafe。
     */
    public Map<String, Object> whoUses() {
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row("JDK 内部（JUC）", "AtomicInteger/Long、ConcurrentHashMap、AQS、LongAdder、ThreadPoolExecutor 的 ctl——用 CAS/偏移量"));
        rows.add(row("Netty", "PooledByteBufAllocator 用 Unsafe 分配/释放堆外内存、直接读写 ByteBuf，零拷贝收发网络数据"));
        rows.add(row("Kafka / Cassandra", "用 Unsafe/堆外内存做大块缓冲与页缓存，减少 GC 停顿"));
        rows.add(row("序列化框架", "Kryo / FST 用 allocateInstance 绕过构造器造对象，提升反序列化速度"));
        rows.add(row("JVM 系新框架", "JDK 9+ 的 ConcurrentHashMap 已改用 VarHandle；高版本框架正逐步迁移"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("tip", "面试聊框架原理时能说出“ConcurrentHashMap 的 Node 数组用 Unsafe/VarHandle 做 CAS 扩容、"
                + "Netty 用 Unsafe 分配堆外内存”，会显得很懂底层。");
        return result;
    }

    private Map<String, Object> risk(String title, String code, String consequence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("code", code);
        m.put("consequence", consequence);
        return m;
    }

    private Map<String, String> row(String k, String v) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("item", k);
        m.put("detail", v);
        return m;
    }
}

package com.example.unsafe.instance;

import com.example.unsafe.common.UnsafeBizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 03. 绕过构造器：{@link Unsafe#allocateInstance(Class)} 不调用任何构造器创建对象。
 *
 * <p>经典用途：高性能反序列化（Kryo）、深拷贝、框架懒初始化；经典风险：
 * 绕过构造器校验/初始化，对象可能处于非法状态（字段全是默认值）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceService {

    private final Unsafe unsafe;

    /**
     * 实验对象：构造器带校验 + 静态计数副作用，方便观察“构造器到底有没有跑”。
     */
    public static class MagicUser {
        private static int constructedCount = 0; // 构造器每跑一次 +1

        private final int id;        // final 字段：构造器之外无法赋值
        private String name;
        private int score = -1;      // 有初始化值的字段

        public MagicUser(int id, String name) {
            if (id <= 0) {
                throw new IllegalArgumentException("id 必须 > 0"); // 构造器校验
            }
            this.id = id;
            this.name = name;
            constructedCount++;
        }

        public static int getConstructedCount() {
            return constructedCount;
        }

        @Override
        public String toString() {
            return "MagicUser{id=" + id + ", name=" + name + ", score=" + score + "}";
        }
    }

    /**
     * 核心演示：new 与 allocateInstance 的对比。
     */
    public Map<String, Object> create() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 常规 new：构造器跑一次，id/name/score 都被赋值，count = 1
        MagicUser byNew = new MagicUser(7, "张三");
        result.put("byNew", byNew.toString());
        result.put("countAfterNew", MagicUser.getConstructedCount());

        // 2. new 负数 id：构造器校验生效，抛异常
        Map<String, Object> newFail = new LinkedHashMap<>();
        try {
            new MagicUser(-1, "非法");
        } catch (IllegalArgumentException e) {
            newFail.put("error", e.getMessage());
        }
        result.put("newWithBadId", newFail);

        // 3. allocateInstance：没有任何构造器被调用！
        MagicUser byUnsafe;
        try {
            byUnsafe = (MagicUser) unsafe.allocateInstance(MagicUser.class);
        } catch (InstantiationException e) {
            throw new UnsafeBizException("allocateInstance 失败（理论上不会发生，因为不需要构造器）：" + e.getMessage(), e);
        }
        result.put("byUnsafe", byUnsafe.toString());
        result.put("countAfterUnsafe", MagicUser.getConstructedCount());

        result.put("conclusion", "allocateInstance 不调构造器：final 字段 id 是默认值 0、name 是 null、"
                + "score 是默认值 0（连初始化 -1 都没执行），构造器静态计数没变。"
                + "绕过校验/初始化的对象可能处于非法状态，这就是它的危险所在。");
        return result;
    }

    /**
     * new vs allocateInstance 对比表。
     */
    public Map<String, Object> compare() {
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row("构造器", "一定执行", "完全不执行"));
        rows.add(row("字段初始值", "构造器里赋的值", "全为类型默认值（0 / null / false）"));
        rows.add(row("final 字段", "可赋值一次", "保持默认值（无法赋值）"));
        rows.add(row("静态初始化", "类加载时执行，与实例无关", "同左（类加载已触发）"));
        rows.add(row("校验逻辑", "构造器里的校验生效", "校验被绕过"));
        rows.add(row("速度", "普通", "更快（省构造器调用，序列化场景明显）"));
        rows.add(row("典型用途", "日常 new", "Kryo 反序列化 / 深拷贝 / 绕过单例 / 框架零参实例化"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("tip", "面试爱问：Kryo 为什么比 JDK 序列化快？——除了更紧凑的格式，还因为它用 allocateInstance 直接造对象，"
                + "不经过构造器/反射的层层校验。");
        return result;
    }

    /**
     * 真实世界谁在用。
     */
    public Map<String, Object> uses() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> uses = new ArrayList<>();
        uses.add("Kryo / FST 等高性能序列化框架：先 allocateInstance 造空对象，再按字段回填，避开构造器开销与校验");
        uses.add("深拷贝工具：绕过构造器直接复制字段，避免拷贝时触发资源初始化");
        uses.add("单例破解演示：allocateInstance(Singleton.class) 能绕过私有构造器拿到“第二个单例”");
        uses.add("框架延迟初始化：如某些 ORM/DI 容器需要无构造器也能实例化代理/增强对象");
        result.put("uses", uses);
        result.put("risk", "反序列化攻击（构造器校验被绕过）是 2015 年 Fastjson/Jackson 高危漏洞的土壤之一，"
                + "所以现代框架在反序列化前会做类白名单校验。");
        return result;
    }

    private Map<String, String> row(String k, String a, String b) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("item", k);
        m.put("byNew", a);
        m.put("byUnsafe", b);
        return m;
    }
}

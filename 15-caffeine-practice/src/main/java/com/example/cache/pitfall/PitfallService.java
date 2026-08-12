package com.example.cache.pitfall;

import com.example.cache.common.CacheBizException;
import com.example.cache.support.HotDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 10. 常见坑与调优：把生产上踩过的缓存坑摆出来讲明白。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PitfallService {

    private final KeyTrapService keyTrapService;
    private final HotDataService hotDataService;

    /**
     * 10 个高频坑（现象 → 原因 → 解法）。
     */
    public Map<String, Object> list() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pitfalls", new Object[]{
                pit("缓存了不该缓存的", "把用户私有/频繁变更/超大对象塞进本地缓存，多实例不一致 + 内存爆", "按数据特性分：只缓存读多写少、可接受短暂不一致的"),
                pit("key 设计用对象 toString", "SpEL key=\"#user\" 使等价对象 key 不同，缓存永远 miss（见 key-demo）", "key 用业务主键：key=\"#user.id\""),
                pit("缓存穿透不防", "不存在的 id 每次都打库，被刷爆", "空值缓存(短TTL) + 布隆过滤器 + 入参校验"),
                pit("热点 key 过期被打爆", "击穿：过期瞬间并发全 miss", "单飞 / 逻辑过期 / 预热（见 05/06 章）"),
                pit("TTL 一刀切", "所有 key 同 TTL，缓存雪崩；或该短的过长", "TTL 加随机抖动错峰；不同数据不同 TTL"),
                pit("先删缓存后更库", "删完缓存、更库前并发读把旧值回填，之后库更新完成但缓存是旧值", "Cache Aside 先更库再删缓存 + 双删"),
                pit("事务内写缓存", "事务回滚了缓存却已更新/删除，缓存与库不一致", "缓存操作放事务提交后（@TransactionalEventListener AFTER_COMMIT）"),
                pit("本地缓存当共享缓存", "多实例各一份，看不到别人写的值", "跨实例共享用 Redis；本地缓存只放每实例独立的热点"),
                pit("预热白做", "预热的 key 超过 maximumSize，热一轮又被淘汰；或预热时机太早被清", "预热数量 ≤ 容量；ApplicationReadyEvent 后预热；配合刷新"),
                pit("只缓存不监控", "命中率暴跌不知道，故障后知后觉", "recordStats + 命中率/淘汰率看板告警（见 04 章）"),
        });
        return result;
    }

    /**
     * SpEL key 陷阱现场：等价对象因 toString 不同导致缓存 miss。
     */
    public Map<String, Object> keyDemo() {
        int beforeBad = keyTrapService.badLoads();
        int beforeGood = keyTrapService.goodLoads();

        // 两个「业务上等价」的对象：同 id=1，但 name 不同（实例不同 → toString 不同）
        Param p1 = new Param(1, "别名A");
        Param p2 = new Param(1, "别名B");

        keyTrapService.queryBad(p1);   // miss → 打库
        keyTrapService.queryBad(p2);   // toString 不同 → key 不同 → 又 miss → 打库
        keyTrapService.queryGood(p1);  // miss → 打库
        keyTrapService.queryGood(p2);  // key=1 相同 → 命中（不打库）

        int badLoads = keyTrapService.badLoads() - beforeBad;
        int goodLoads = keyTrapService.goodLoads() - beforeGood;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("p1", p1.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(p1)));
        result.put("p2", p2.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(p2)));
        result.put("badKeyStyle", "key=\"#param\"（整个对象）");
        result.put("badDbLoads", badLoads);
        result.put("goodKeyStyle", "key=\"#param.id\"（业务主键）");
        result.put("goodDbLoads", goodLoads);
        result.put("conclusion", "两个等价的 Param 对象，key=\"#param\" 时 toString 不同 → 2 次都打库；"
                + "key=\"#param.id\" 时命中同一个缓存 → 只打库 1 次。");
        result.put("tip", "缓存 key 永远用业务主键（id/编号），别用对象本身或 toString——这是缓存「像没开一样」的头号原因。");
        return result;
    }

    /**
     * 调优要点。
     */
    public Map<String, Object> tuning() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", new String[]{
                "容量：maximumSize 按「热点数据量 + 30% 余量」设，别拍脑袋；配完看 evictionCount 是否持续高位",
                "TTL：热点数据 expireAfterWrite 短 + refreshAfterWrite 自动续新；冷数据 TTL 长点",
                "预热：启动/发版后自动预热热门 key，把 miss 提前到空闲期（见 05 章）",
                "监控：recordStats + 命中率/淘汰率/加载失败率看板；命中率<90% 要查原因",
                "本地缓存只放进程内热点：用户列表/报表这类大对象放 Redis 或干脆不缓存",
                "写操作与事务解耦：缓存删/写在事务提交后执行，别让回滚留下脏缓存",
                "大促/秒杀：预热 + 单飞 + TTL 抖动三件套，提前压测缓存容量"
        });
        result.put("memory", "Caffeine 是堆内存：预估 每 key 平均几十字节 × 容量，多实例各占一份；"
                + "超大缓存给 JVM 留足堆，否则触发频繁 GC。");
        result.put("tip", "缓存调优的顺序：先看命中率 → 再看淘汰率 → 最后才动容量/TTL，别盲调。");
        return result;
    }

    private Map<String, Object> pit(String name, String cause, String fix) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("cause", cause);
        m.put("fix", fix);
        return m;
    }
}

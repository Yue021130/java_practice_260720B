package com.example.cache.consistency;

import com.example.cache.support.CacheLogStore;
import com.example.cache.support.HotDataService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 09. 缓存一致性：Cache Aside 为什么「先更库、再删缓存」、双删怎么来的。
 *
 * 核心矛盾：缓存是 DB 的副本，副本和主库之间永远存在不一致窗口，
 * 我们能做的是「收窄窗口」而不是消灭窗口。删缓存比更新缓存安全，
 * 因为删了最多多查一次库，更新错了就是脏数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsistencyService {

    private final Cache<String, Object> l1Cache;
    private final HotDataService hotDataService;
    private final CacheLogStore logStore;

    /**
     * Cache Aside 现场：只更库不删缓存 → 脏数据；更库 + 删缓存 → 自愈。
     */
    public Map<String, Object> asideDemo(int id) {
        String key = HotDataService.KEY_PREFIX + id;

        // 1. 初始：读 → miss → 查库 → 回填缓存
        Map<String, Object> original = hotDataService.loadUser(id);
        l1Cache.put(key, original);

        // 2. 业务更新库（模拟另一个请求改了 DB）
        Map<String, Object> newUser = new LinkedHashMap<>(original);
        newUser.put("name", "库里的新名字-" + id);
        hotDataService.saveUser(id, newUser);

        // 3. 只更库、不删缓存 → 缓存里还是旧值（脏数据）
        Map<String, Object> staleRead = (Map<String, Object>) l1Cache.getIfPresent(key);

        // 4. 正确做法：删缓存（Cache Aside 写路径）
        l1Cache.invalidate(key);

        // 5. 再读 → miss → 查库拿到新值 → 回填
        Map<String, Object> fresh = hotDataService.loadUser(id);
        l1Cache.put(key, fresh);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("cacheBeforeDelete", staleRead);
        result.put("cacheAfterDeleteReload", fresh);
        result.put("staleIfOnlyUpdateDb", staleRead != null
                && "库里的新名字-" + id != null
                && !("库里的新名字-" + id).equals(staleRead.get("name")));
        result.put("tip", "只更库不删缓存，读到的还是旧值（脏数据）；更库后删缓存，下次读自动加载新值——"
                + "这就是 Cache Aside「先更库、再删缓存」的原因。");

        logStore.add("consistency", "aside-demo", key, true, "Cache Aside 读写路径");
        return result;
    }

    /**
     * 双删演示：写前删 + 写后删，压掉「读旧值回填」的竞态窗口。
     */
    public Map<String, Object> doubleDeleteDemo(int id) {
        String key = HotDataService.KEY_PREFIX + id;
        List<String> timeline = new ArrayList<>();

        l1Cache.invalidate(key);
        timeline.add("① 写前删缓存");

        // 模拟竞态：写操作执行期间，一个读请求把旧值回填进了缓存
        Map<String, Object> old = hotDataService.loadUser(id);
        l1Cache.put(key, old);
        timeline.add("②（竞态）读请求把旧值回填进缓存");

        Map<String, Object> newUser = new LinkedHashMap<>(old);
        newUser.put("name", "双删后的新名字-" + id);
        hotDataService.saveUser(id, newUser);
        timeline.add("③ 更新 DB（此时缓存里是旧值）");

        l1Cache.invalidate(key);
        timeline.add("④ 写后删缓存（双删的第二删）");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("timeline", timeline);
        result.put("tip", "双删 = 写前删 + 写后删：第二删把 ③ 之前被回填的旧值清掉。"
                + "延迟双删再把第二删延迟几百 ms，等竞态读彻底结束，进一步收窄窗口（Redis 场景常用）。");
        return result;
    }

    /**
     * 一致性模式对比。
     */
    public Map<String, Object> patterns() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patterns", new LinkedHashMap<String, Object>() {{
            put("Cache Aside（旁路缓存）", "读 miss 查库回填；写先更库再删缓存。最常用、最可控（本项目）");
            put("Read Through", "缓存层代理查库：业务只读缓存，miss 由缓存组件去 DB 加载");
            put("Write Through", "写操作先同步写缓存再写库（或反向），业务无感，但吞吐低");
            put("Write Behind（写回）", "先写缓存、异步批量落库：吞吐最高，但可能丢数据，需补偿");
        }});
        result.put("ranking", "一致性：Write Through ≈ Read Through > Cache Aside > Write Behind；"
                + "吞吐恰好相反。业务多数选 Cache Aside，因为可控、简单。");
        result.put("tip", "没有银弹：能接受最终一致就用 Cache Aside + 短 TTL；强一致场景干脆别用缓存。");
        return result;
    }

    /**
     * 一致性速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("whyDeleteNotUpdate", "更新缓存有顺序风险：先更库后更缓存，中间读到旧缓存=脏；"
                + "先更缓存后更库，缓存可能是错的。删缓存则简单粗暴——下次读重新加载，最多多查一次库。");
        result.put("inconsistencySources", new String[]{
                "并发写：两个请求同时改同一 key，缓存与库的更新顺序不一致",
                "读回填竞态：删缓存后、更新库完成前，旧值被读请求回填（双删/延迟双删解决）",
                "多级缓存：L1 各实例各一份，天然不一致（短 TTL 兜底）"
        });
        result.put("solutions", new String[]{
                "Cache Aside + 双删（写前删 + 写后延迟删）",
                "Binlog 订阅：监听 DB 变更（Canal/CDC），异步同步/删除缓存，最可靠",
                "版本号/时间戳比对：缓存带版本，版本落后就刷新",
                "最终一致兜底：短 TTL，让错误数据自然过期"
        });
        result.put("tip", "面试答一致性：先说「缓存与 DB 永远存在窗口」，再讲 Cache Aside + 双删 + binlog，就够用了。");
        return result;
    }
}

package com.example.cache.twolevel;

import com.example.cache.support.CacheLogStore;
import com.example.cache.support.HotDataService;
import com.example.cache.support.SimpleRedisCache;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 07. 两级缓存：L1 本地 Caffeine + L2 分布式缓存（这里用内存 Map 模拟 Redis）。
 *
 * 为什么两级：
 * - L1（Caffeine，进程内）：最快（纳秒级），但每个实例各有一份、数据会不一致；
 * - L2（Redis，分布式）：跨实例共享、一致性好，但要一次网络 RTT；
 * 组合起来：绝大多数请求打在 L1，L1 miss 才去 L2，L2 miss 才查库——吞吐与一致性折中。
 *
 * 读路径：L1 → L2 → DB（逐级回填）；写路径：Cache Aside（先更库，再删 L1 和 L2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoLevelService {

    private final Cache<String, Object> l1Cache;
    private final SimpleRedisCache redis;
    private final HotDataService hotDataService;
    private final CacheLogStore logStore;

    /**
     * 读路径：L1 → L2 → DB，命中哪一级从哪一级回填。
     */
    public Map<String, Object> get(int id) {
        String key = HotDataService.KEY_PREFIX + id;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);

        // 1. L1 本地缓存
        Object l1 = l1Cache.getIfPresent(key);
        if (l1 != null) {
            result.put("source", "L1(Caffeine)");
            result.put("value", l1);
            result.put("tip", "L1 命中：纳秒级返回，连 Redis 都不用碰。");
            logStore.add("twolevel", "get", key, true, "L1 命中");
            return result;
        }

        // 2. L2 分布式缓存（模拟 Redis）
        Object l2 = redis.get(key);
        if (l2 != null) {
            l1Cache.put(key, l2); // 回填 L1，下次更快
            result.put("source", "L2(Redis)");
            result.put("value", l2);
            result.put("tip", "L2 命中：跨实例共享的数据，回填 L1 后后续请求走本地。");
            logStore.add("twolevel", "get", key, true, "L2 命中回填 L1");
            return result;
        }

        // 3. 都没命中 → 查 DB → 逐级回填
        Map<String, Object> user = hotDataService.loadUser(id);
        if (user != null) {
            redis.put(key, user, 30_000);   // L2 设 TTL 30s
            l1Cache.put(key, user);         // L1 由配置 expireAfterWrite 控制
        }
        result.put("source", "DB");
        result.put("value", user);
        result.put("tip", "L1/L2 都 miss → 查库并逐级回填；下次读 L1 命中。");
        logStore.add("twolevel", "get", key, false, "DB 回填 L1+L2");
        return result;
    }

    /**
     * 写路径：Cache Aside —— 先更库，再删 L1 + L2。
     */
    public Map<String, Object> put(int id, String name, String dept) {
        String key = HotDataService.KEY_PREFIX + id;
        Map<String, Object> current = hotDataService.userDb().get(id);
        if (current == null) {
            throw new com.example.cache.common.CacheBizException("用户不存在: " + id);
        }
        Map<String, Object> updated = new LinkedHashMap<>(current);
        updated.put("name", name);
        updated.put("dept", dept);
        hotDataService.saveUser(id, updated);

        // Cache Aside：先更库，再删缓存（而不是更新缓存）
        l1Cache.invalidate(key);
        redis.delete(key);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("updated", updated);
        result.put("action", "更新 DB → 删除 L1 + L2");
        result.put("tip", "为什么不更新缓存而删缓存？更新缓存与并发写容易造成数据错乱（先写库后写缓存顺序不一致），"
                + "删缓存让「下次读」重新加载，简单且安全。");
        logStore.add("twolevel", "put", key, null, "Cache Aside 写路径");
        return result;
    }

    /**
     * 删两级缓存。
     */
    public Map<String, Object> evict(int id) {
        String key = HotDataService.KEY_PREFIX + id;
        l1Cache.invalidate(key);
        redis.delete(key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("action", "删除 L1 + L2");
        result.put("tip", "手动清缓存：改配置/修 Bug 后强制让数据重新加载用。");
        logStore.add("twolevel", "evict", key, null, "删两级缓存");
        return result;
    }

    /**
     * 一致性策略说明。
     */
    public Map<String, Object> consistency() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategies", new LinkedHashMap<String, Object>() {{
            put("Cache Aside", "读写都绕过缓存：读 miss 查库回填，写先更库再删缓存（本项目采用的）");
            put("双删", "删除缓存时删两次（写前删 + 写后短暂延迟再删），压掉「读旧值回填」与「写新值」的竞态窗口");
            put("延迟双删", "双删的第二次删除延迟 500ms~1s，等读请求把旧值回填完再删，进一步收窄窗口（Redis 场景常用）");
            put("Read Through / Write Through", "缓存层代理数据库：读穿/写穿由缓存组件负责，业务无感，但依赖缓存中间件能力");
            put("Write Behind", "先写缓存/异步批量落库：吞吐最高，但可能丢数据，需要补偿机制");
        }});
        result.put("consistency", "两级缓存的一致性天然比单 Redis 弱（每实例 L1 各自一份）；"
                + "要求不高的数据用「短 TTL + 定期刷新」最终一致即可，别为强一致把架构搞复杂。");
        result.put("tip", "本模块 L1 TTL 10s、L2 TTL 30s——就算更新漏删了某层，也会被 TTL 兜底自愈。");
        return result;
    }

    /**
     * 两级缓存速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("why", "L1 快但多实例不一致，L2 一致但有网络开销；两级缓存 = 热点走本地、冷数据走共享，吞吐最高。");
        result.put("levels", new LinkedHashMap<String, Object>() {{
            put("L1 Caffeine", "maximumSize 小(200)、TTL 短(10s)：本地热点，命中率 90%+");
            put("L2 Redis", "共享缓存，TTL 中(30s)：跨实例兜底");
            put("DB", "最终数据源，只在 L1/L2 都 miss 时被碰");
        }});
        result.put("watchouts", new String[]{
                "写入要双删（L1 与 L2 都删），否则读路径会把旧值回填",
                "L1 是进程内缓存：发布新版本/扩容实例时每台都要重新预热",
                "本地缓存慎放超大对象（用户列表、报表），内存扛不住",
                "监控要分 L1/L2 两套命中率，才能定位问题在哪一层"
        });
        result.put("tip", "秒杀/商品详情这类「读极多、写少、一致性要求不高」的场景是两级缓存的最佳舞台。");
        return result;
    }
}

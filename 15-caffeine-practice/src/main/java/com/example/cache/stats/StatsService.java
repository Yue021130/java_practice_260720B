package com.example.cache.stats;

import com.example.cache.support.CacheLogStore;
import com.example.cache.support.HotDataService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 04. 统计与监控：recordStats() 打开后，命中率 / 加载耗时 / 淘汰数一目了然。
 *
 * 生产上这几个指标直接对接到监控平台（Micrometer/Prometheus），
 * 命中率骤降 = 缓存有问题（key 过期 / 被清 / 键设计变了）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final Cache<String, Object> demoCache;
    private final HotDataService hotDataService;
    private final CacheLogStore logStore;

    /**
     * 跑一批「读缓存，miss 就查库回填」的访问，再看统计指标。
     */
    public Map<String, Object> demo(int accesses) {
        int safe = Math.max(10, Math.min(accesses, 5000));
        demoCache.invalidateAll();

        // 模拟真实读路径：50 个热点 key，随机访问 N 次
        int dbLoadsBefore = hotDataService.userLoadCount();
        for (int i = 1; i <= safe; i++) {
            int id = i % 50 + 1;
            String key = HotDataService.KEY_PREFIX + id;
            Object value = demoCache.getIfPresent(key);
            if (value == null) {
                demoCache.put(key, hotDataService.loadUser(id));
            }
        }
        int dbLoads = hotDataService.userLoadCount() - dbLoadsBefore;

        CacheStats stats = demoCache.stats();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accesses", safe);
        result.put("distinctKeys", 50);
        result.put("dbLoads", dbLoads);
        result.put("metrics", new LinkedHashMap<String, Object>() {{
            put("requestCount", stats.requestCount());
            put("hitCount", stats.hitCount());
            put("missCount", stats.missCount());
            put("hitRate", round(stats.hitRate()));
            put("evictionCount", stats.evictionCount());
            put("loadCount", stats.loadCount());
            put("totalLoadTimeMs", stats.totalLoadTime() / 1_000_000);
            put("averageLoadTimeMs", stats.loadCount() == 0 ? 0.0
                    : round(stats.totalLoadTime() / 1_000_000.0 / stats.loadCount()));
        }});
        result.put("tip", safe + " 次访问只有 " + dbLoads + " 次真正查库（约 " + round(100.0 * dbLoads / safe) + "%），"
                + "其余全部命中缓存——这就是缓存的价值：把 30ms 的慢查询变成 <1ms 的内存读。");

        logStore.add("stats", "demo", "user:*", null, "统计采样 " + safe + " 次访问");
        return result;
    }

    /**
     * 统计指标速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enable", "Caffeine 默认不统计，需要构建时 .recordStats()（本项目 demoCache/loadingCache 已打开）。");
        result.put("metrics", new LinkedHashMap<String, Object>() {{
            put("hitRate / hitCount / missCount", "命中率与命中数——监控缓存的健康度，命中率骤降要告警");
            put("loadCount / loadFailureCount", "加载次数与失败次数——失败率上升说明数据源或缓存配置有问题");
            put("totalLoadTime / averageLoadTime", "加载总耗时与平均耗时——评估缓存对查询的加速效果");
            put("evictionCount", "淘汰数量——淘汰太快说明 maximumSize 设小了，命中率会被拖低");
            put("requestCount", "总请求数");
        }});
        result.put("monitor", "生产上通过 Micrometer 把 stats 暴露给 Prometheus，Grafana 配命中率/淘汰率看板；"
                + "命中率 < 90% 通常意味着缓存设计有问题（TTL 太短 / 键太散 / 容量不足）。");
        result.put("tip", "用 cache.policy().eviction() 可看当前容量水位；多实例各有一份本地缓存，指标要按实例聚合。");
        return result;
    }

    private double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}

package com.example.scp.service;

import com.example.scp.cache.CacheMetrics;
import com.example.scp.cache.TwoLevelCache;
import com.example.scp.cache.TwoLevelCacheManager;
import com.example.scp.dto.CacheStatsVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 缓存监控 Service：聚合二级缓存的命中率统计与 key 分布，
 * 供 /api/cache 下的管理接口调用，配合前端监控面板直观看到
 * 「L1 命中 / L2 命中 / miss / 命中率」的实际效果。
 */
@Service
public class CacheMonitorService {

    /** 已知的缓存名列表（缓存是惰性创建的，不枚举名字就无法发现还没用到的缓存） */
    private static final List<String> KNOWN_CACHES = Arrays.asList("product", "productPage");

    private final TwoLevelCacheManager cacheManager;

    public CacheMonitorService(TwoLevelCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 所有已知缓存的命中率统计快照。
     */
    public List<CacheStatsVO> stats() {
        List<CacheStatsVO> list = new ArrayList<>();
        for (String name : KNOWN_CACHES) {
            CacheMetrics.Snapshot snapshot = cacheManager.getTwoLevelCache(name).getMetrics().snapshot();
            CacheStatsVO vo = new CacheStatsVO();
            vo.setName(name);
            vo.setL1Hit(snapshot.getL1Hit());
            vo.setL2Hit(snapshot.getL2Hit());
            vo.setMiss(snapshot.getMiss());
            vo.setEvict(snapshot.getEvict());
            vo.setHitRate(snapshot.getHitRate());
            list.add(vo);
        }
        return list;
    }

    /**
     * 查看指定缓存的两级 key 分布：l1Keys 是 JVM 内 Caffeine 的 key，
     * l2Keys 是 Redis 中的 key（已去掉 "cacheName::" 前缀）。
     */
    public Map<String, List<String>> keys(String cache) {
        TwoLevelCache twoLevelCache = cacheManager.getTwoLevelCache(cache);
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("l1Keys", new ArrayList<>(twoLevelCache.l1Keys()));
        result.put("l2Keys", twoLevelCache.l2Keys().stream().sorted().collect(Collectors.toList()));
        return result;
    }

    /**
     * 清空指定缓存（两级都清）。
     */
    public void clear(String cache) {
        cacheManager.getTwoLevelCache(cache).clear();
    }
}

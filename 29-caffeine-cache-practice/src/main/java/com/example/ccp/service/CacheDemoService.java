package com.example.ccp.service;

import com.example.ccp.dto.UserVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 缓存演示 Service：读取 Caffeine 原生统计信息，支撑 /api/cache/** 实验接口。
 */
@Service
public class CacheDemoService {

    @Resource
    private UserService userService;
    @Resource
    private CacheManager cacheManager;

    /**
     * 汇总 user / userPage 两个缓存的统计信息（recordStats 开启后才有数据）。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String name : new String[]{"user", "userPage"}) {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            Map<String, Object> item = new LinkedHashMap<>();
            if (cache != null && cache.getNativeCache() instanceof Cache) {
                Cache<Object, Object> nativeCache = (Cache<Object, Object>) cache.getNativeCache();
                CacheStats s = nativeCache.stats();
                item.put("estimatedSize", nativeCache.estimatedSize());
                item.put("hitCount", s.hitCount());
                item.put("missCount", s.missCount());
                item.put("hitRate", String.format("%.2f%%", s.hitRate() * 100));
            } else {
                item.put("exists", cache != null);
            }
            result.put(name, item);
        }
        return result;
    }

    /**
     * 对比实验：同一 id 连续查两次——第一次 miss 走数据库，第二次 hit 直接返回。
     * 结合控制台 SQL 日志与 /api/cache/stats 的命中率变化，直观看到缓存生效。
     */
    public Map<String, Object> compare(Long id) {
        long start1 = System.currentTimeMillis();
        UserVO first = userService.getById(id);
        long cost1 = System.currentTimeMillis() - start1;

        long start2 = System.currentTimeMillis();
        UserVO second = userService.getById(id);
        long cost2 = System.currentTimeMillis() - start2;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("firstFromDatabase", first != null);
        result.put("secondFromCache", second != null);
        result.put("firstCostMs", cost1);
        result.put("secondCostMs", cost2);
        result.put("tip", "第二次未打印 SQL 日志即命中缓存；编辑/删除用户后缓存被清空，需重新加载");
        return result;
    }

    /** 清空两个缓存，便于重复实验 */
    public void clear() {
        for (String name : new String[]{"user", "userPage"}) {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}

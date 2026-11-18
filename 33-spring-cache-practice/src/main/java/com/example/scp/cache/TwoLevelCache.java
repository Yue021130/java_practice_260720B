package com.example.scp.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 二级缓存实现：Caffeine（一级，JVM 内） + Redis（二级，跨进程）。
 *
 * <p>查找顺序：L1 → L2 → 回源（由调用方，通常是 @Cacheable 的方法体）。
 * 写缓存时两级都写；L2 命中会回填 L1，下次同 key 走 L1 纳秒级返回。</p>
 *
 * <p>三个经典问题的落点（详见 README 八股）：</p>
 * <ul>
 *   <li>穿透：业务 null 转 {@link NullValue}，TTL 短（nullTtl）</li>
 *   <li>击穿：热点 key 过期瞬间的并发重建由 {@link RedisLockClient} 互斥（见 ProductService 演示）</li>
 *   <li>雪崩：put 时 TTL = ttl + 随机抖动，避免同刻大面积过期</li>
 * </ul>
 */
public class TwoLevelCache implements org.springframework.cache.Cache {

    private final String name;
    private final RedisTemplate<String, Object> redisTemplate;
    /** 一级缓存：expireAfterWrite 固定短 TTL，简单可靠 */
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> l1;
    private final TwoLevelCacheProperties properties;
    private final CacheMetrics metrics = new CacheMetrics();

    public TwoLevelCache(String name,
                         RedisTemplate<String, Object> redisTemplate,
                         TwoLevelCacheProperties properties) {
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        // 一级 TTL 固定为二级基础 TTL 的 1/10 且不超过 60s：L1 是「快进快出」的缓冲层，
        // 数据变更靠 evict 主动失效，不靠 L1 自身过期（过期只是兜底）
        long l1Ttl = Math.min(propertiesFor(name).getTtl() / 10, 60);
        this.l1 = Caffeine.newBuilder()
                .maximumSize(properties.getL1MaximumSize())
                .expireAfterWrite(Math.max(l1Ttl, 5), TimeUnit.SECONDS)
                .build();
    }

    private TwoLevelCacheProperties.Config propertiesFor(String cacheName) {
        return properties.getConfigs().getOrDefault(
                cacheName, defaultConfig(properties.getDefaultTtl()));
    }

    private static TwoLevelCacheProperties.Config defaultConfig(long ttl) {
        TwoLevelCacheProperties.Config c = new TwoLevelCacheProperties.Config();
        c.setTtl(ttl);
        c.setJitter(0);
        return c;
    }

    /** Redis key 遵循 Spring Cache 的 "cacheName::key" 惯例，便于用 Redis 客户端直接观察 */
    private String redisKey(Object key) {
        return name + "::" + key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        Object value = getRaw(key);
        return value == null ? null : () -> value;
    }

    /**
     * 二级缓存查找主逻辑。
     * 返回值统一「业务视角」：命中空值占位符时返回 null（调用方视为 miss，但已挡住 DB）。
     */
    private Object getRaw(Object key) {
        String k = String.valueOf(key);
        // ① 一级缓存
        Object value = l1.getIfPresent(k);
        if (value != null) {
            metrics.l1Hit();
            return unwrap(value);
        }
        // ② 二级缓存
        value = redisTemplate.opsForValue().get(redisKey(key));
        if (value != null) {
            metrics.l2Hit();
            // 回填一级：下次同 key 直接 L1 命中（刷新 L1 的 TTL）
            l1.put(k, value);
            return unwrap(value);
        }
        // ③ 两级都 miss，回源由 @Cacheable 的方法体完成
        metrics.miss();
        return null;
    }

    /** 空值占位符 → null（业务视角）；正常值原样返回 */
    private Object unwrap(Object value) {
        return NullValue.INSTANCE.equals(value) ? null : value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Object value = getRaw(key);
        return value == null ? null : type.cast(value);
    }

    /**
     * Cache 接口约定的「读穿」方法：命中直接返回，未命中调用 valueLoader
     * 回源并写入缓存后返回。Spring 的缓存注解底层会用到。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
        Object value = getRaw(key);
        if (value != null) {
            return (T) value;
        }
        try {
            T loaded = valueLoader.call();
            put(key, loaded);
            return loaded;
        } catch (java.lang.Exception e) {
            throw new java.lang.IllegalStateException(
                    "缓存回源失败 key=" + redisKey(key), e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        String k = String.valueOf(key);
        Object stored = (value == null) ? NullValue.INSTANCE : value;
        l1.put(k, stored);
        // 空值占位符必须远短于正常 TTL：占位只是短期挡穿透，
        // 如果按正常 TTL 存 10 分钟，数据中途真实写入后会有较长的不一致窗口
        long ttl = (value == null) ? properties.getNullTtl() : ttlWithJitter();
        redisTemplate.opsForValue().set(redisKey(key), stored, ttl, TimeUnit.SECONDS);
    }

    /**
     * 实际 TTL = 基础 TTL + [0, jitter) 随机抖动。
     * 若所有 key 用同一 TTL，批量加载的 key 会同一秒过期，形成雪崩；
     * 加抖动把过期时刻打散。jitter=0 的配置（如分布式锁）不抖动。
     */
    private long ttlWithJitter() {
        TwoLevelCacheProperties.Config cfg = propertiesFor(name);
        long jitter = cfg.getJitter();
        return cfg.getTtl() + (jitter <= 0 ? 0 : (long) (Math.random() * jitter));
    }

    @Override
    public void evict(Object key) {
        metrics.evict();
        l1.invalidate(String.valueOf(key));
        redisTemplate.delete(redisKey(key));
    }

    @Override
    public void clear() {
        metrics.evict();
        l1.invalidateAll();
        // 不用 keys 命令（生产禁用，O(n) 阻塞），改用 scan 游标遍历匹配
        String pattern = name + "::*";
        org.springframework.data.redis.core.ScanOptions options =
                org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(redisTemplate::delete);
        }
    }

    /** 监控用：一级缓存当前 key 集合 */
    public java.util.Set<String> l1Keys() {
        return l1.asMap().keySet();
    }

    /** 监控用：二级缓存 key 集合（scan 匹配，返回去掉 "name::" 前缀的业务 key） */
    public java.util.Set<String> l2Keys() {
        String pattern = name + "::*";
        org.springframework.data.redis.core.ScanOptions options =
                org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(100).build();
        java.util.Set<String> keys = new java.util.HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(full -> keys.add(full.substring(name.length() + 2)));
        }
        return keys;
    }

    public CacheMetrics getMetrics() {
        return metrics;
    }
}

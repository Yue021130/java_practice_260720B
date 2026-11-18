package com.example.scp.cache;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 二级缓存配置项（对应 application.yml 的 spring.cache.two-level）。
 *
 * <p>每个 cacheName 一条配置：ttl 为二级缓存基础 TTL（秒），
 * jitter 为随机抖动上限（秒）——实际 TTL = ttl + Random[0, jitter]，
 * 防止大量 key 同一时刻过期造成「缓存雪崩」。</p>
 */
@Data
public class TwoLevelCacheProperties {

    /** cacheName → 配置 */
    private Map<String, Config> configs = new LinkedHashMap<>();

    /** 空值占位符 TTL（秒），防穿透用，必须远短于正常值 TTL */
    private long nullTtl = 30;

    /** 一级缓存（Caffeine）最大条目数 */
    private long l1MaximumSize = 1000;

    /** 找不到对应配置时使用的默认 TTL */
    private long defaultTtl = 300;

    @Data
    public static class Config {
        /** 基础 TTL（秒） */
        private long ttl;
        /** 随机抖动上限（秒），0 表示不抖动 */
        private long jitter;
    }
}

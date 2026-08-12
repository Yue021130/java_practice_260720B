package com.example.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存实践自定义配置（前缀 cache.practice，见 application.yml）。
 *
 * 用 @EnableConfigurationProperties 注册为 Bean，各 Service 注入读取，
 * 让「预热开关 / key 数 / 模拟 DB 耗时」等演示参数可调，也方便测试里覆盖。
 */
@Data
@ConfigurationProperties(prefix = "cache.practice")
public class CachePracticeProperties {

    /** 预热配置 */
    private Preheat preheat = new Preheat();

    /** 通用缓存最大容量 */
    private int maxSize = 1000;

    /** 通用缓存写入后过期时间（毫秒） */
    private long expireAfterWriteMs = 60000;

    /** LoadingCache 的刷新间隔（毫秒）：超过该时长读已存在的 key 会异步刷新 */
    private long refreshAfterWriteMs = 2000;

    /** 模拟「从 DB 查询」的单次耗时（毫秒），越大越能看清缓存效果 */
    private long loadCostMs = 30;

    @Data
    public static class Preheat {

        /** 启动完成后是否自动预热 */
        private boolean enabled = true;

        /** 预热的热门 key 数量 */
        private int keyCount = 50;

        /** 每批加载多少条（分批进行，避免启动卡顿） */
        private int batchSize = 10;
    }
}

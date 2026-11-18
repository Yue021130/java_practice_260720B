package com.example.scp.dto;

import lombok.Data;

/**
 * 缓存监控出参：单个缓存的命中统计快照。
 *
 * <p>hitRate 为 0~1 的小数，前端展示时 *100 转百分比。</p>
 */
@Data
public class CacheStatsVO {

    /** 缓存名（product / productPage） */
    private String name;

    /** 一级缓存（Caffeine）命中次数：纳秒级路径 */
    private long l1Hit;

    /** 二级缓存（Redis）命中次数：跨进程共享，命中后回填 L1 */
    private long l2Hit;

    /** 两级都 miss、回源数据库的次数 */
    private long miss;

    /** 主动驱逐次数（@CacheEvict / 手动清空） */
    private long evict;

    /** 命中率 = (l1Hit + l2Hit) / 总请求 */
    private double hitRate;
}

package com.example.scp.cache;

import lombok.Value;

import java.util.concurrent.atomic.LongAdder;

/**
 * 单缓存命中率统计：L1 命中 / L2 命中 / 双 miss（回源 DB）/ 驱逐次数。
 *
 * <p>用 LongAdder 而不是 AtomicLong：高并发读写下 LongAdder 采用
 * 分段思想，多线程累加几乎无竞争，适合命中率统计这种「写多读少」场景。
 * 每个 TwoLevelCache 实例持有一个，监控接口再按 cacheName 聚合展示。</p>
 */
public class CacheMetrics {

    private final LongAdder l1Hit = new LongAdder();
    private final LongAdder l2Hit = new LongAdder();
    private final LongAdder miss = new LongAdder();
    private final LongAdder evict = new LongAdder();

    /** L1（Caffeine）命中：最快路径，纳秒级 */
    public void l1Hit() {
        l1Hit.increment();
    }

    /** L2（Redis）命中：比 DB 快，且会回填 L1，下次就是 L1 命中 */
    public void l2Hit() {
        l2Hit.increment();
    }

    /** 两级都 miss，回源数据库 */
    public void miss() {
        miss.increment();
    }

    /** 主动驱逐次数（@CacheEvict / 手动清空） */
    public void evict() {
        evict.increment();
    }

    public Snapshot snapshot() {
        long l1 = l1Hit.sum();
        long l2 = l2Hit.sum();
        long m = miss.sum();
        long total = l1 + l2 + m;
        // 命中率 = (L1+L2 命中) / 总请求；evict 不参与命中率
        double hitRate = total == 0 ? 0.0 : (double) (l1 + l2) / total;
        return new Snapshot(l1, l2, m, evict.sum(), hitRate);
    }

    @Value
    public static class Snapshot {
        long l1Hit;
        long l2Hit;
        long miss;
        long evict;
        /** 0~1 之间，前端展示时 *100 转百分比 */
        double hitRate;
    }
}

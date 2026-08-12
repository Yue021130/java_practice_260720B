package com.example.cache.preheat;

import com.example.cache.config.CachePracticeProperties;
import com.example.cache.support.CacheLogStore;
import com.example.cache.support.HotDataService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 05. 缓存预热（重点场景）：应用启动后把热门数据先装进缓存。
 *
 * 为什么需要预热：
 * 冷启动时缓存是空的，前 N 个请求全部 miss、全部打到数据库——这就是「缓存击穿」的最初形态。
 * 预热就是在流量进来之前，把「最可能被访问的 key」（热门商品/字典/配置）主动加载进缓存。
 *
 * 实现要点：
 * - 监听 {@link ApplicationReadyEvent}（应用完全就绪后）自动预热，而不是在启动过程中阻塞；
 * - 用 AtomicBoolean 保证「正在预热中」的幂等（重复触发直接返回当前状态）；
 * - 分批加载（batchSize）避免一次性阻塞太久；
 * - 记录预热前命中率（冷启动≈0）与预热后探测命中率，量化预热收益；
 * - 状态机 PENDING → RUNNING → SUCCESS / FAILED，前端可随时查。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachePreheatService {

    /** 预热状态机 */
    public enum State {
        /** 尚未预热 */
        PENDING,
        /** 预热中 */
        RUNNING,
        /** 预热成功 */
        SUCCESS,
        /** 预热失败 */
        FAILED
    }

    private final CachePracticeProperties props;
    private final Cache<String, Object> preheatCache;
    private final HotDataService hotDataService;
    private final CacheLogStore logStore;

    private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

    /** 是否正在预热（防并发重复触发） */
    private final AtomicBoolean warming = new AtomicBoolean(false);

    private final AtomicLong preheatLoaded = new AtomicLong();

    private volatile long startTime;
    private volatile long endTime;
    private volatile int keyCount;
    private volatile int batchCount;
    private volatile String error;
    /** 预热前探测命中数/总数（冷启动时缓存为空，通常 0/20） */
    private volatile int preWarmProbeHits;
    private volatile int preWarmProbeTotal;

    /**
     * 应用完全就绪后自动预热（开关见 cache.practice.preheat.enabled）。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (props.getPreheat().isEnabled()) {
            log.info("应用启动完成，开始自动预热 {} 个热门 key ...", props.getPreheat().getKeyCount());
            warm();
        } else {
            log.info("缓存预热已关闭（cache.practice.preheat.enabled=false），可在前端手动触发");
        }
    }

    /**
     * 执行预热（可手动触发 /api/preheat/warm 重复调用，预热中幂等返回）。
     */
    public Map<String, Object> warm() {
        if (!warming.compareAndSet(false, true)) {
            Map<String, Object> busy = status();
            busy.put("tip", "预热进行中，已忽略本次触发（幂等）。");
            return busy;
        }
        try {
            state.set(State.RUNNING);
            startTime = System.currentTimeMillis();
            preheatLoaded.set(0);
            batchCount = 0;
            error = null;
            // 预热前真实探测命中率（冷启动时缓存为空 → 0/20 命中）。
            // 注意：Caffeine 的 stats().hitRate() 在「零请求」时返回 1.0（乐观约定），
            // 会掩盖冷启动真相，所以这里用真实探测而不是 stats().hitRate()。
            Probe preWarm = probeHotKeys();
            preWarmProbeHits = preWarm.hits;
            preWarmProbeTotal = preWarm.total;

            int total = props.getPreheat().getKeyCount();
            int batchSize = props.getPreheat().getBatchSize();

            // 数据源：这里直接「查库」拿热门数据（真实工程是查 DB/大数据平台的热门清单）
            List<Map<String, Object>> hot = hotDataService.hotUsers(total);
            for (int i = 0; i < hot.size(); i++) {
                Map<String, Object> user = hot.get(i);
                Integer id = (Integer) user.get("id");
                // 预热核心动作：把热门 key 直接写进缓存，而不是等第一个请求来 miss 再加载
                preheatCache.put(HotDataService.KEY_PREFIX + id, user);
                preheatLoaded.incrementAndGet();
                if ((i + 1) % batchSize == 0) {
                    batchCount++;
                }
            }
            if (hot.size() % batchSize != 0) {
                batchCount++;
            }
            keyCount = hot.size();
            endTime = System.currentTimeMillis();
            state.set(State.SUCCESS);
            Probe after = probeHotKeys();
            log.info("缓存预热完成：{} 个 key，耗时 {}ms（预热前探测 {}/{} 命中 → 预热后 {}/{} 命中）",
                    keyCount, endTime - startTime,
                    preWarmProbeHits, preWarmProbeTotal, after.hits, after.total);
            logStore.add("preheat", "warm", "preheat", null, "预热 " + keyCount + " 个热门 key");
            return status();
        } catch (Exception e) {
            state.set(State.FAILED);
            error = e.getMessage();
            log.error("缓存预热失败", e);
            return status();
        } finally {
            warming.set(false);
        }
    }

    /**
     * 预热状态。
     */
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state", state.get().name());
        result.put("enabled", props.getPreheat().isEnabled());
        result.put("keyCount", keyCount);
        result.put("loaded", preheatLoaded.get());
        result.put("batchSize", props.getPreheat().getBatchSize());
        result.put("batchCount", batchCount);
        result.put("durationMs", endTime > 0 ? (endTime - startTime) : 0);
        result.put("error", error);
        result.put("tip", "PENDING 未预热 / RUNNING 预热中 / SUCCESS 完成 / FAILED 失败；"
                + "状态由事件监听 + AtomicBoolean 保证并发安全。");
        return result;
    }

    /**
     * 预热收益：对热门 key 跑一轮探测读，对比预热前与当前的真实命中数。
     */
    public Map<String, Object> stats() {
        Probe now = probeHotKeys();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state", state.get().name());
        result.put("preWarmProbeHits", preWarmProbeHits);
        result.put("preWarmProbeTotal", preWarmProbeTotal);
        result.put("preWarmHitRate", round(preWarmProbeTotal == 0 ? 0.0
                : (double) preWarmProbeHits / preWarmProbeTotal));
        result.put("probeReads", now.total);
        result.put("probeHits", now.hits);
        result.put("probeMisses", now.total - now.hits);
        result.put("probeHitRate", round((double) now.hits / now.total));
        result.put("dbLoadsSaved", "本次探测 " + now.hits + " 次命中无需查库，每次省 " + props.getLoadCostMs() + "ms");
        result.put("tip", "冷启动时预热前探测 0/20 命中；预热后热门 key 探测读基本全命中。"
                + "生产上预热后命中率应长期稳定在高位，若持续走低说明热门清单/淘汰策略没跟上。");
        return result;
    }

    /**
     * 当前预热配置。
     */
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", props.getPreheat().isEnabled());
        result.put("keyCount", props.getPreheat().getKeyCount());
        result.put("batchSize", props.getPreheat().getBatchSize());
        result.put("maxSize", props.getMaxSize());
        result.put("expireAfterWriteMs", props.getExpireAfterWriteMs());
        result.put("loadCostMs", props.getLoadCostMs());
        result.put("tip", "改 application.yml 的 cache.practice.preheat.* 可调；测试里关掉自动预热改用手动 warm。");
        return result;
    }

    /**
     * 预热速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("why", "冷启动缓存为空，前 N 个请求全部 miss 打爆数据库；预热把「流量进来前的空窗期」填上，"
                + "尤其适合秒杀、大促、每日 9 点上班高峰这类「可预见的集中访问」。");
        result.put("how", new String[]{
                "静态预热：启动时把固定清单（字典/配置/热榜）直接 put 进缓存（本模块演示）",
                "动态预热：启动时查 DB 拿「热门 key 清单」（按访问量排序），再逐批加载",
                "定时预热：周期任务（@Scheduled）在流量高峰前 30 分钟刷新热门数据"
        });
        result.put("timing", "用 ApplicationReadyEvent（应用就绪后）而不是启动过程中硬等——"
                + "预热失败不能把应用启动搞挂；异步/分批执行，别阻塞就绪探针。");
        result.put("fallback", new String[]{
                "预热失败要记录并告警，缓存空了还能靠「miss 时加载」兜底，只是慢",
                "预热的数据量别超过 maximumSize，否则「热了一轮又被淘汰」白干",
                "配合 refreshAfterWrite：预热后定时刷新，让热门数据长期新鲜"
        });
        result.put("monitor", "预热完成后看命中率曲线：理想是上线即高位；命中率爬坡缓慢 = 预热 key 没选对。");
        result.put("tip", "预热的本质是「把 miss 从流量高峰期提前到空闲期」——换时间，不换总工作量。");
        return result;
    }

    /**
     * 对热门 key 做一轮探测读（不查库、不加载，只 getIfPresent 计数）。
     */
    private Probe probeHotKeys() {
        int probe = 20;
        int total = Math.min(props.getPreheat().getKeyCount(), 50);
        int hits = 0;
        for (int i = 1; i <= probe; i++) {
            int id = i % total + 1;
            if (preheatCache.getIfPresent(HotDataService.KEY_PREFIX + id) != null) {
                hits++;
            }
        }
        return new Probe(hits, probe);
    }

    private double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    /** 探测结果：命中数 / 探测总数 */
    private static final class Probe {
        private final int hits;
        private final int total;

        private Probe(int hits, int total) {
            this.hits = hits;
            this.total = total;
        }
    }
}

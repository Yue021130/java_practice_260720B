package com.example.cache;

import com.example.cache.preheat.CachePreheatService;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存预热专项测试。
 *
 * 关闭启动自动预热，全部改用手动 warm，便于断言状态机与命中率提升。
 * key-count=20 / batch-size=5 / load-cost-ms=1，跑得快且确定性。
 */
@SpringBootTest(properties = {
        "cache.practice.preheat.enabled=false",
        "cache.practice.preheat.key-count=20",
        "cache.practice.preheat.batch-size=5",
        "cache.practice.load-cost-ms=1"
})
class PreheatTest {

    @Autowired
    private CachePreheatService preheatService;

    @Autowired
    @Qualifier("preheatCache")
    private Cache<String, Object> preheatCache;

    @Test
    void warmSucceedsWithExpectedCounts() {
        Map<String, Object> status = preheatService.warm();
        assertThat(status.get("state")).isEqualTo("SUCCESS");
        assertThat(((Number) status.get("keyCount")).intValue()).isEqualTo(20);
        assertThat(((Number) status.get("loaded")).intValue()).isEqualTo(20);
        assertThat(((Number) status.get("batchCount")).intValue()).isEqualTo(4); // 20 / 5
        assertThat(status.get("error")).isNull();
    }

    @Test
    void hitRateImprovesAfterWarm() {
        preheatCache.invalidateAll(); // 清成冷缓存

        Map<String, Object> before = preheatService.stats();
        assertThat((Integer) before.get("probeHits")).isZero(); // 冷缓存探测全 miss

        preheatService.warm();

        Map<String, Object> after = preheatService.stats();
        assertThat((Integer) after.get("preWarmProbeHits")).isZero();   // 预热前(冷缓存)探测 0 命中
        assertThat((Integer) after.get("probeHits")).isEqualTo(20);     // 20 个热门 key 全命中
        assertThat((Double) after.get("probeHitRate")).isEqualTo(1.0);
        assertThat(after.get("state")).isEqualTo("SUCCESS");
    }

    @Test
    void concurrentWarmIsSafeAndIdempotent() throws Exception {
        int threads = 4;
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    ready.await();
                    preheatService.warm(); // 并发触发
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    done.countDown();
                }
            }).start();
        }
        ready.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get()).isNull();

        Map<String, Object> status = preheatService.status();
        assertThat(status.get("state")).isEqualTo("SUCCESS");
        assertThat(status.get("keyCount")).isEqualTo(20);
    }
}

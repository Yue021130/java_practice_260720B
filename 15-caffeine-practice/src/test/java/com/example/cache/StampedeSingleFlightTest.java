package com.example.cache;

import com.example.cache.stampede.StampedeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 穿透/击穿/单飞并发专项测试。
 *
 * 用真实并发线程验证两个确定性结论：
 * - 无保护（stampede-demo）：N 个并发 → N 次打库；
 * - 单飞（singleflight）：N 个并发 → 只打库 1 次。
 */
@SpringBootTest(properties = {
        "cache.practice.preheat.enabled=false",
        // 加载耗时 50ms：保证 latch 放行的所有线程都在第一次加载完成前到达 computeIfAbsent，
        // 从而确定性断言「N 并发只加载 1 次」（加载太快会有线程错峰新开一轮，那是正确语义）
        "cache.practice.load-cost-ms=50"
})
class StampedeSingleFlightTest {

    @Autowired
    private StampedeService stampedeService;

    @Test
    void stampedeHitsDbForEveryThread() {
        int threads = 10;
        Map<String, Object> result = stampedeService.stampedeDemo(threads);
        assertThat((Integer) result.get("dbLoads")).isEqualTo(threads);
    }

    @Test
    void singleFlightLoadsOnlyOnce() {
        int threads = 20;
        Map<String, Object> result = stampedeService.singleflight(threads);
        assertThat((Integer) result.get("threads")).isEqualTo(threads);
        // actualLoads 来自 AtomicLong，是 Long；统一按 Number 取值
        assertThat(((Number) result.get("actualLoads")).intValue()).isEqualTo(1);
    }
}

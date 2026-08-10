package com.example.comm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全场景接口集成测试。
 *
 * 覆盖 10 个章节的 JSON 接口。测试参数刻意调小（线程数、耗时、数据量），
 * 避免多线程演示拖慢测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sharedScenarios() throws Exception {
        ok("/api/shared/volatile-demo?workers=4&flagDelayMs=50");
        ok("/api/shared/atomic-demo?threads=4&increments=500");
        ok("/api/shared/explain");
    }

    @Test
    void waitNotifyScenarios() throws Exception {
        ok("/api/waitnotify/producer-consumer?productions=10&capacity=3");
        ok("/api/waitnotify/explain");
    }

    @Test
    void conditionScenarios() throws Exception {
        ok("/api/condition/bounded-buffer?productions=10&capacity=3");
        ok("/api/condition/signal-demo?waiters=4");
        ok("/api/condition/explain");
    }

    @Test
    void cooperateScenarios() throws Exception {
        ok("/api/cooperate/join-demo?tasks=3&taskMs=40");
        ok("/api/cooperate/interrupt-demo?mode=sleep");
        ok("/api/cooperate/interrupt-demo?mode=loop");
        ok("/api/cooperate/explain");
    }

    @Test
    void lockSupportScenarios() throws Exception {
        ok("/api/locksupport/park-unpark?delayMs=50");
        ok("/api/locksupport/unpark-first");
        ok("/api/locksupport/explain");
    }

    @Test
    void syncScenarios() throws Exception {
        ok("/api/sync/latch-demo?workers=3");
        ok("/api/sync/barrier-demo?parties=3&rounds=2");
        ok("/api/sync/semaphore-demo?permits=2&threads=6");
        ok("/api/sync/exchanger-demo");
        ok("/api/sync/phaser-demo?parties=3");
        ok("/api/sync/explain");
    }

    @Test
    void queueScenarios() throws Exception {
        ok("/api/queue/blocking-demo?productions=10&capacity=3");
        ok("/api/queue/family");
        ok("/api/queue/explain");
    }

    @Test
    void asyncScenarios() throws Exception {
        ok("/api/async/future-demo?taskMs=30");
        ok("/api/async/cf-demo?taskMs=30");
        ok("/api/async/cf-combine?tasks=3");
        ok("/api/async/explain");
    }

    @Test
    void pipeScenarios() throws Exception {
        ok("/api/pipe/piped-demo?messages=5");
        ok("/api/pipe/cross-process");
        ok("/api/pipe/explain");
    }

    @Test
    void summaryScenarios() throws Exception {
        ok("/api/summary/overview");
        ok("/api/summary/decision-table");
        ok("/api/summary/unified-model");
        ok("/api/summary/explain");
    }

    private void ok(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":200");
    }
}

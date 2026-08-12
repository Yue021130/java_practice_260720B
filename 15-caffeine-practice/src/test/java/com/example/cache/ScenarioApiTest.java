package com.example.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全场景接口集成测试。
 *
 * 覆盖 10 个章节的 JSON 接口。测试参数刻意调小：
 * - 关闭启动自动预热（预热专项见 PreheatTest）
 * - load-cost-ms=5、refresh-after-write-ms=100，避免接口里的 sleep 拖慢测试
 */
@SpringBootTest(properties = {
        "cache.practice.preheat.enabled=false",
        "cache.practice.load-cost-ms=5",
        "cache.practice.refresh-after-write-ms=100"
})
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void basicScenarios() throws Exception {
        ok("/api/basic/cache-demo?id=1");
        ok("/api/basic/loading?id=1");
        ok("/api/basic/info");
    }

    @Test
    void evictionScenarios() throws Exception {
        ok("/api/eviction/size-demo?count=12");
        // 时间淘汰：小 duration，快
        ok("/api/eviction/expire-demo?type=write&durationMs=60");
        ok("/api/eviction/expire-demo?type=access&durationMs=60");
        ok("/api/eviction/explain");
    }

    @Test
    void refreshScenarios() throws Exception {
        ok("/api/refresh/refresh-demo");       // waitMs=0 → 等 refreshAfterWrite(100)+100
        ok("/api/refresh/async-demo?id=1");
        ok("/api/refresh/explain");
    }

    @Test
    void statsScenarios() throws Exception {
        ok("/api/stats/demo?accesses=200");
        ok("/api/stats/explain");
    }

    @Test
    void preheatScenarios() throws Exception {
        ok("/api/preheat/status");
        ok("/api/preheat/config");
        ok("/api/preheat/explain");
        // 手动触发一次，验证能跑到 SUCCESS
        MvcResult warm = mockMvc.perform(post("/api/preheat/warm"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(warm.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"state\":\"SUCCESS\"");
        ok("/api/preheat/stats");
    }

    @Test
    void stampedeScenarios() throws Exception {
        ok("/api/stampede/overview");
        ok("/api/stampede/null-demo?times=20");
        ok("/api/stampede/stampede-demo?threads=10");
        ok("/api/stampede/singleflight?threads=10");
        ok("/api/stampede/explain");
    }

    @Test
    void twoLevelScenarios() throws Exception {
        ok("/api/twolevel/get?id=1");
        okPost("/api/twolevel/put?id=1&name=接口改的名&dept=接口改的部");
        okPost("/api/twolevel/evict?id=1");
        ok("/api/twolevel/consistency");
        ok("/api/twolevel/explain");
    }

    @Test
    void springCacheScenarios() throws Exception {
        ok("/api/spring/query?id=10");
        okPost("/api/spring/update?id=10&name=注解改的名");
        okPost("/api/spring/delete?id=10");
        okPost("/api/spring/multi?id=10");
        ok("/api/spring/explain");
    }

    @Test
    void consistencyScenarios() throws Exception {
        ok("/api/consistency/aside-demo?id=4");
        ok("/api/consistency/double-delete-demo?id=5");
        ok("/api/consistency/patterns");
        ok("/api/consistency/explain");
    }

    @Test
    void pitfallScenarios() throws Exception {
        ok("/api/pitfall/list");
        MvcResult key = mockMvc.perform(get("/api/pitfall/key-demo"))
                .andExpect(status().isOk())
                .andReturn();
        // 陷阱现场：错误 key 写法打库 2 次，正确写法只打 1 次
        assertThat(key.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"badDbLoads\":2")
                .contains("\"goodDbLoads\":1");
        ok("/api/pitfall/tuning");
    }

    private void ok(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":200");
    }

    private void okPost(String url) throws Exception {
        MvcResult result = mockMvc.perform(post(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":200");
    }
}

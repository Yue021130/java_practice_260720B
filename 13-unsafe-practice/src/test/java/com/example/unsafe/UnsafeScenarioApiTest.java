package com.example.unsafe;

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
 * 覆盖 8 个章节的全部接口：初识 / 堆外内存 / 绕过构造器 / CAS / 字段偏移 / park-unpark / 内存屏障 / 本质。
 * benchmark 用较小的次数，避免测试过慢。
 */
@SpringBootTest
@AutoConfigureMockMvc
class UnsafeScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void introScenarios() throws Exception {
        ok("/api/intro/info");
        ok("/api/intro/getunsafe-demo");
        ok("/api/intro/why");
    }

    @Test
    void memoryScenarios() throws Exception {
        okPost("/api/memory/allocate?count=5");
        ok("/api/memory/setcopy");
        okPost("/api/memory/leak?blocks=2");
    }

    @Test
    void instanceScenarios() throws Exception {
        ok("/api/instance/create");
        ok("/api/instance/compare");
        ok("/api/instance/uses");
    }

    @Test
    void casScenarios() throws Exception {
        okPost("/api/cas/spin?times=10000");
        okPost("/api/cas/benchmark?threads=2&times=50000");
        ok("/api/cas/aba");
        ok("/api/cas/explain");
    }

    @Test
    void offsetScenarios() throws Exception {
        ok("/api/offset/fields");
        ok("/api/offset/directwrite");
        ok("/api/offset/array");
        ok("/api/offset/layout");
    }

    @Test
    void parkScenarios() throws Exception {
        ok("/api/park/demo");
        ok("/api/park/compare");
        ok("/api/park/explain");
    }

    @Test
    void fenceScenarios() throws Exception {
        ok("/api/fence/demo");
        ok("/api/fence/explain");
    }

    @Test
    void essenceScenarios() throws Exception {
        ok("/api/essence/risks");
        ok("/api/essence/essence");
        ok("/api/essence/evolution");
        ok("/api/essence/whouses");
    }

    /**
     * 关键断言：cas/aba 场景必须能稳定复现出"无版本号成功、带版本号失败"的结局。
     */
    @Test
    void abaDetectsVersionChange() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cas/aba"))
                .andExpect(status().isOk())
                .andReturn();
        // 显式按 UTF-8 解码：MockMvc 的 getContentAsString() 默认用 ISO-8859-1，会读乱中文
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body)
                .contains("\"useVersion\":false")
                .contains("\"useVersion\":true")
                .contains("\"aCasSucceeded\":true")
                .contains("\"aCasSucceeded\":false")
                .contains("ABA 被识别");
    }

    private void ok(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    private void okPost(String url) throws Exception {
        mockMvc.perform(post(url)).andExpect(status().isOk());
    }
}

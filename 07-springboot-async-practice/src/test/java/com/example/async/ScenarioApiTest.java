package com.example.async;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

@SpringBootTest
@AutoConfigureMockMvc
public class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allScenariosShouldReturn200AndNonEmptyData() throws Exception {
        String[] endpoints = {
                "/api/async/fire-forget",
                "/api/async/completable-future",
                "/api/async/future-timeout",
                "/api/async/pool-config",
                "/api/async/custom-executor",
                "/api/async/rejected",
                "/api/async/exception",
                "/api/async/self-invocation",
                "/api/async/context-propagation",
                "/api/async/batch-aggregate",
                "/api/async/metrics",
                "/api/async/graceful-shutdown",
                "/api/async/sync-vs-async"
        };

        for (String endpoint : endpoints) {
            MvcResult result = mockMvc.perform(post(endpoint))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isNotEmpty())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .as("%s 的响应应包含 interviewNote", endpoint)
                    .contains("interviewNote");
        }

        // 异步 Controller 需要 asyncDispatch 才能拿到最终响应体
        MvcResult asyncResult = mockMvc.perform(post("/api/async/controller-async"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.released").value(true))
                .andExpect(jsonPath("$.data.interviewNote").exists());
    }

    @Test
    void rejectedShouldBeGreaterThanZero() throws Exception {
        mockMvc.perform(post("/api/async/rejected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rejected").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void selfInvocationShouldRunOnSameThread() throws Exception {
        mockMvc.perform(post("/api/async/self-invocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sameThread").value(true));
    }

    @Test
    void customExecutorShouldMatchExpectedPrefix() throws Exception {
        mockMvc.perform(post("/api/async/custom-executor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cpuMatch").value(true))
                .andExpect(jsonPath("$.data.ioMatch").value(true));
    }

    @Test
    void contextPropagationShouldEqualTraceId() throws Exception {
        mockMvc.perform(post("/api/async/context-propagation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.propagationOk").value(true));
    }

    @Test
    void batchAggregateSumShouldBeCorrect() throws Exception {
        mockMvc.perform(post("/api/async/batch-aggregate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sum").value(150)); // 10+20+30+40+50
    }
}

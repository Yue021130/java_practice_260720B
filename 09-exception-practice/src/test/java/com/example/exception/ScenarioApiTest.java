package com.example.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全场景接口集成测试。
 *
 * 调用所有实验接口，验证返回状态与数据结构。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hierarchyScenarios() throws Exception {
        ok("/api/hierarchy/family");
        okPost("/api/hierarchy/checked-unchecked?checked=false");
        okPost("/api/hierarchy/custom-exception?throwWithCause=false");
        ok("/api/hierarchy/when-to-use");
    }

    @Test
    void basicsScenarios() throws Exception {
        okPost("/api/basics/execution-order?scenario=normal");
        okPost("/api/basics/finally-override?withReturn=true");
        okPost("/api/basics/try-with-resources?businessFail=false&closeFail=false");
        okPost("/api/basics/exception-chain");
        okPost("/api/basics/mask-sensitive");
        ok("/api/basics/finally-not-execute");
    }

    @Test
    void commonExceptionScenarios() throws Exception {
        okPost("/api/common/npe");
        okPost("/api/common/class-cast");
        okPost("/api/common/number-format");
        okPost("/api/common/index-out-of-bounds");
        okPost("/api/common/cme");
        okPost("/api/common/uoe");
        okPost("/api/common/no-such-element");
        okPost("/api/common/stack-overflow");
        ok("/api/common/oom");
        okPost("/api/common/class-not-found");
        ok("/api/common/assertion");
    }

    @Test
    void advancedScenarios() throws Exception {
        okPost("/api/advanced/multi-catch");
        okPost("/api/advanced/rethrow");
        okPost("/api/advanced/lambda-checked");
        okPost("/api/advanced/stream-exception");
        okPost("/api/advanced/suppressed");
        okPost("/api/advanced/exception-masking");
        okPost("/api/advanced/performance");
    }

    @Test
    void springScenarios() throws Exception {
        ok("/api/spring/error-code");

        // 业务异常应被全局处理器捕获，返回 400
        mockMvc.perform(post("/api/spring/business-error"))
                .andExpect(status().isBadRequest());

        // 参数校验失败
        mockMvc.perform(post("/api/spring/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"email\":\"not-email\"}"))
                .andExpect(status().isBadRequest());

        // ResponseStatusException 返回 403
        mockMvc.perform(post("/api/spring/response-status"))
                .andExpect(status().isForbidden());

        // 未知异常兜底 500
        mockMvc.perform(post("/api/spring/unknown-error"))
                .andExpect(status().isInternalServerError());

        // 参数校验成功
        mockMvc.perform(post("/api/spring/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"a@b.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void concurrencyScenarios() throws Exception {
        okPost("/api/concurrency/thread-uncaught");
        okPost("/api/concurrency/uncaught-handler");
        okPost("/api/concurrency/future-get");
        okPost("/api/concurrency/completable-exception");
        okPost("/api/concurrency/async-exception");
        okPost("/api/concurrency/pool-swallow");
    }

    @Test
    void bestPracticeScenarios() throws Exception {
        okPost("/api/bestpractice/swallow");
        okPost("/api/bestpractice/flow-control");
        okPost("/api/bestpractice/fail-fast");
        okPost("/api/bestpractice/logging");
        okPost("/api/bestpractice/transaction");
    }

    private void ok(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":200");
    }

    private void okPost(String url) throws Exception {
        MvcResult result = mockMvc.perform(post(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":200");
    }
}

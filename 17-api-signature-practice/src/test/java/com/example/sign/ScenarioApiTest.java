package com.example.sign;

import com.example.sign.config.SignAuthInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 覆盖 10 个章节的 JSON 接口 + 拦截器真实鉴权链路
 * （受保护接口：未带签名 401，带合法签名 200）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void principleScenarios() throws Exception {
        ok("/api/principle/elements");
        ok("/api/principle/flow");
        ok("/api/principle/vs-apikey");
        ok("/api/principle/explain");
    }

    @Test
    void signScenarios() throws Exception {
        ok("/api/sign/compute?method=GET&uri=/api/v1/users&query=page=1&size=20");
        ok("/api/sign/compute?method=POST&uri=/api/v1/users&body=%7B%22name%22%3A%22z%22%7D");
        ok("/api/sign/canonical");
        ok("/api/sign/explain");
    }

    @Test
    void verifyScenarios() throws Exception {
        // 不篡改 → 通过
        MvcResult ok = mockMvc.perform(get("/api/verify/demo?tamper=none"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(ok.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"passed\":true");

        // 篡改 body → 失败
        MvcResult tampered = mockMvc.perform(get("/api/verify/demo?tamper=body"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(tampered.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"passed\":false");
        ok("/api/verify/explain");
    }

    @Test
    void timestampScenarios() throws Exception {
        // 当前时间 → 通过
        MvcResult now = mockMvc.perform(get("/api/timestamp/demo?timestamp=now"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(now.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"passed\":true");

        // 一小时前 → 拒绝
        MvcResult expired = mockMvc.perform(get("/api/timestamp/demo?timestamp=-3600"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(expired.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"passed\":false");
        ok("/api/timestamp/explain");
    }

    @Test
    void nonceScenarios() throws Exception {
        // 第一次占用成功，第二次（同 nonce）拒绝
        MvcResult result = mockMvc.perform(get("/api/nonce/demo?nonce=test-nonce-001"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"firstAcquire\":true")
                .contains("\"secondAcquire\":false");
        ok("/api/nonce/explain");
    }

    @Test
    void bodyScenarios() throws Exception {
        // 不篡改 → 完整
        MvcResult clean = mockMvc.perform(get("/api/body/demo?tamper=false"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(clean.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"passed\":true");

        // 篡改 body → 完整性失配
        MvcResult tampered = mockMvc.perform(get("/api/body/demo?tamper=true"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(tampered.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"passed\":false");
        ok("/api/body/explain");
    }

    @Test
    void canonicalScenarios() throws Exception {
        ok("/api/canonical/query-sort");
        ok("/api/canonical/headers-sort");
        ok("/api/canonical/uri-encoding");
        ok("/api/canonical/explain");
    }

    @Test
    void simplifiedScenarios() throws Exception {
        ok("/api/simplified/demo?uri=/api/v1/order/query&params=orderNo=20240701001&page=1");
        ok("/api/simplified/explain");
    }

    @Test
    void interceptorScenarios() throws Exception {
        // 1. 不带签名访问受保护接口 → 401
        mockMvc.perform(get("/api/interceptor/protected"))
                .andExpect(status().isUnauthorized());

        // 2. 生成签名 → 带 X- 头访问 → 200
        MvcResult gen = mockMvc.perform(get("/api/interceptor/generate"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(gen.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("data");
        String appId = data.get("appId").asText();
        String timestamp = data.get("timestamp").asText();
        String nonce = data.get("nonce").asText();
        String signature = data.get("signature").asText();

        mockMvc.perform(get("/api/interceptor/protected")
                        .header(SignAuthInterceptor.HEADER_APP_ID, appId)
                        .header(SignAuthInterceptor.HEADER_TIMESTAMP, timestamp)
                        .header(SignAuthInterceptor.HEADER_NONCE, nonce)
                        .header(SignAuthInterceptor.HEADER_SIGNATURE, signature))
                .andExpect(status().isOk());

        // 3. 闭环演示：正常通过 / 篡改拒绝
        MvcResult ok = mockMvc.perform(get("/api/interceptor/secure-demo?tamper=false"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(ok.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"passed\":true");
        MvcResult tampered = mockMvc.perform(get("/api/interceptor/secure-demo?tamper=true"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(tampered.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"passed\":false");

        ok("/api/interceptor/explain");
    }

    @Test
    void summaryScenarios() throws Exception {
        ok("/api/summary/overview");
        ok("/api/summary/compare");
        ok("/api/summary/principles");
        ok("/api/summary/pitfalls");
        ok("/api/summary/explain");
    }

    private void ok(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":200");
    }
}

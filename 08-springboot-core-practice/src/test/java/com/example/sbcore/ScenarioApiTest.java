package com.example.sbcore;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testAllScenarios() throws Exception {
        assertScenario("/api/core/starters", "Starter");
        assertScenario("/api/core/auto-config-beans", "自动装配");
        assertScenario("/api/core/config-priority", "命令行");
        assertScenario("/api/core/config-props", "dev-user");
        assertScenario("/api/core/bean-lifecycle", "构造器实例化");
        assertScenario("/api/core/conditional", "true");
        assertScenario("/api/core/cache-caffeine-basic", "Caffeine");
        assertScenario("/api/core/cache-ops", "cacheable");
        assertScenarioWithParam("/api/core/cache-hit", "totalRequests=30", "hitRate");
        assertScenario("/api/core/cache-redis", "Redis");
        assertScenario("/api/core/cache-compare", "Caffeine");
        assertScenario("/api/core/actuator", "health");
    }

    @Test
    void testConfigPriorityProfile() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/core/config-priority"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("appName").asText()).isEqualTo("sbcore-dev");
        assertThat(json.get("data").get("activeProfiles").toString()).contains("dev");
    }

    @Test
    void testConfigPropsBinding() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/core/config-props"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("appProperties").get("userName").asText()).isEqualTo("dev-user");
        assertThat(json.get("data").get("customProperties").get("threadPoolSize").asInt()).isBetween(1, 65535);
    }

    @Test
    void testConditionalBeans() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/core/conditional"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("onFeatureBeanRegistered").asBoolean()).isTrue();
        assertThat(json.get("data").get("onClassBeanRegistered").asBoolean()).isTrue();
        assertThat(json.get("data").get("onMissingBeanRegistered").asBoolean()).isTrue();
    }

    @Test
    void testCacheHitRate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/core/cache-hit").param("totalRequests", "50"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("totalRequests").asInt()).isEqualTo(50);
        assertThat(json.get("data").get("hitRate").asText()).endsWith("%");
    }

    private void assertScenario(String endpoint, String expectedSnippet) throws Exception {
        MvcResult result = mockMvc.perform(post(endpoint))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("interviewNote").asText()).isNotBlank();
        assertThat(json.get("data").toString()).contains(expectedSnippet);
    }

    private void assertScenarioWithParam(String endpoint, String param, String expectedSnippet) throws Exception {
        String[] parts = param.split("=");
        MvcResult result = mockMvc.perform(post(endpoint).param(parts[0], parts[1]))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("interviewNote").asText()).isNotBlank();
        assertThat(json.get("data").toString()).contains(expectedSnippet);
    }
}

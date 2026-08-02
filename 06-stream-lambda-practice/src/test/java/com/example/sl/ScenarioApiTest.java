package com.example.sl;

import com.example.sl.common.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String[] ENDPOINTS = {
            "/api/lambda/functional",
            "/api/lambda/method-ref",
            "/api/stream/create",
            "/api/stream/intermediate",
            "/api/stream/terminal",
            "/api/collectors/group-partition",
            "/api/collectors/join-summary",
            "/api/optional/safe",
            "/api/stream/primitive",
            "/api/parallel/speedup",
            "/api/parallel/overhead",
            "/api/parallel/race-condition",
            "/api/parallel/correct-reduce",
            "/api/parallel/order-findany"
    };

    @Test
    void allScenariosReturnSuccess() throws Exception {
        for (String endpoint : ENDPOINTS) {
            MvcResult result = mockMvc.perform(post(endpoint))
                    .andExpect(status().isOk())
                    .andReturn();
            String json = result.getResponse().getContentAsString();
            ApiResponse<?> response = objectMapper.readValue(json, ApiResponse.class);
            assertThat(response.getCode()).as("端点 %s 返回 code 应为 200", endpoint).isEqualTo(200);
            assertThat(response.getData()).as("端点 %s 应返回 data", endpoint).isNotNull();
        }
    }

    @Test
    void raceConditionProducesMismatch() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/parallel/race-condition"))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        ApiResponse<Map<String, Object>> response = objectMapper.readValue(json, new TypeReference<ApiResponse<Map<String, Object>>>() {});

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();

        Integer expected = (Integer) response.getData().get("expected");
        Integer actual = (Integer) response.getData().get("actual");
        assertThat(expected).isEqualTo(100000);
        assertThat(actual).isNotEqualTo(expected);
    }

    @Test
    void correctReduceEqualsExpected() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/parallel/correct-reduce"))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        ApiResponse<Map<String, Object>> response = objectMapper.readValue(json, new TypeReference<ApiResponse<Map<String, Object>>>() {});

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();

        Long expected = ((Number) response.getData().get("expected")).longValue();
        Long reduceSum = ((Number) response.getData().get("reduceSum")).longValue();
        Long collectSum = ((Number) response.getData().get("collectSum")).longValue();

        assertThat(expected).isEqualTo(500000500000L);
        assertThat(reduceSum).isEqualTo(expected);
        assertThat(collectSum).isEqualTo(expected);
    }
}

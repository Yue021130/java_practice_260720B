package com.example.tl;

import com.example.tl.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 场景接口全量冒烟测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void allScenariosReturnSuccess() throws Exception {
        String[] endpoints = {
                "/api/basic/isolation",
                "/api/basic/initial",
                "/api/web/user-context",
                "/api/web/mdc-trace",
                "/api/web/dateformat-safe",
                "/api/cross/inheritable",
                "/api/cross/pool-hazard",
                "/api/cross/pool-remove",
                "/api/cross/async-context",
                "/api/cross/ttl-propagation",
                "/api/advance/leak-analysis",
                "/api/advance/best-practice"
        };

        for (String endpoint : endpoints) {
            MvcResult result = mockMvc.perform(post(endpoint))
                    .andExpect(status().isOk())
                    .andReturn();
            String json = result.getResponse().getContentAsString();
            ApiResponse<?> response = objectMapper.readValue(json, ApiResponse.class);
            assertThat(response.getCode()).as("端点 %s 返回 code 应为 200", endpoint).isEqualTo(200);
            assertThat(response.getData()).as("端点 %s 应返回 data", endpoint).isNotNull();
        }
    }
}

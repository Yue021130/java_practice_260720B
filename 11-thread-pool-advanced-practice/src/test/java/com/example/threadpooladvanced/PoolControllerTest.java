package com.example.threadpooladvanced;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void metricsShouldReturnPredefinedPools() throws Exception {
        mockMvc.perform(get("/api/pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void submitToTinyPoolShouldReturnMetrics() throws Exception {
        String body = "{\"count\":2,\"taskDurationMs\":100}";
        mockMvc.perform(post("/api/pool/predefined/tinyPool/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.poolId").value("tinyPool"));
    }

    @Test
    void createCustomPoolShouldSucceed() throws Exception {
        String body = "{\"poolId\":\"testCreate\",\"corePoolSize\":1,\"maximumPoolSize\":2,\"keepAliveTime\":0,\"timeUnit\":\"SECONDS\",\"queueCapacity\":3,\"queueType\":\"ArrayBlockingQueue\",\"rejectionPolicy\":\"AbortPolicy\",\"threadFactoryPrefix\":\"test\"}";
        mockMvc.perform(post("/api/pool/custom/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.poolId").value("testCreate"));
    }
}

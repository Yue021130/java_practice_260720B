package com.example.bd.controller;

import com.example.bd.dto.CalcRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 金额计算 Controller 测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AmountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calculate_shouldReturnTotal() throws Exception {
        CalcRequest req = new CalcRequest();
        req.setPrice(new BigDecimal("99.99"));
        req.setQuantity(3);
        req.setDiscount(new BigDecimal("0.9"));
        req.setTaxRate(new BigDecimal("6"));
        req.setScale(2);

        mockMvc.perform(post("/api/amount/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalAmount").value("286.17"));
    }

    @Test
    void pitfalls_shouldReturnDoubleProblem() throws Exception {
        mockMvc.perform(get("/api/amount/pitfalls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data['new BigDecimal(0.1)']").exists());
    }
}

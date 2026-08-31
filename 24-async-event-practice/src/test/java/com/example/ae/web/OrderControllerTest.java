package com.example.ae.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单 Controller 测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndPay_shouldReturnAsyncTip() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/order/create")
                        .param("userId", "1")
                        .param("amount", BigDecimal.valueOf(99.99).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").exists())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String orderNo = JsonPath.read(response, "$.data.orderNo");

        mockMvc.perform(post("/api/order/pay").param("orderNo", orderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("已支付"));

        TimeUnit.SECONDS.sleep(2);

        mockMvc.perform(get("/api/order/notify-logs").param("orderNo", orderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logCount").value(3));
    }
}

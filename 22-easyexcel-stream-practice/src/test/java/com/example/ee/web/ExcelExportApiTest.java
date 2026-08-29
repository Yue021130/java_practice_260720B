package com.example.ee.web;

import com.example.ee.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Excel 导出接口集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExcelExportApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void clean() {
        orderRepository.deleteAll();
    }

    @Test
    void generate_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/excel/generate").param("count", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.actualCount").value(50));
    }

    @Test
    void exportStream_shouldReturnExcel_whenHasData() throws Exception {
        mockMvc.perform(post("/api/excel/generate").param("count", "50"));

        mockMvc.perform(get("/api/excel/export/stream"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    org.assertj.core.api.Assertions.assertThat(contentType)
                            .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                });
    }

    @Test
    void exportStream_shouldReturnBusinessError_whenNoData() throws Exception {
        mockMvc.perform(get("/api/excel/export/stream"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("没有数据")));
    }

    @Test
    void explain_shouldReturnEightLegEssay() throws Exception {
        mockMvc.perform(get("/api/excel/explain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").exists());
    }
}

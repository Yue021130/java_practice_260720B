package com.example.caa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bean Validation 参数校验测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ValidationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validUserShouldPass() throws Exception {
        String validUser = "{\"name\":\"张三\",\"phone\":\"13800138001\",\"email\":\"zhangsan@example.com\",\"idCard\":\"110101199001011234\"}";
        MvcResult result = mockMvc.perform(post("/api/demo/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUser))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"code\":200");
        assertThat(body).contains("校验通过");
    }

    @Test
    void invalidUserShouldReturn400() throws Exception {
        String invalidUser = "{\"name\":\"\",\"phone\":\"123\",\"email\":\"invalid\",\"idCard\":\"xxx\"}";
        MvcResult result = mockMvc.perform(post("/api/demo/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUser))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"code\":400");
    }
}

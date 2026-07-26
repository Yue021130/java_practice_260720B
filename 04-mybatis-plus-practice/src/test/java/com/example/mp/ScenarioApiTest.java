package com.example.mp;

import com.example.mp.common.ApiResponse;
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
 *
 * 遍历所有 POST 端点，验证返回 HTTP 200、统一响应 code=200、data 非空。
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
                // 实体类注解
                "/api/entity/table-name",
                "/api/entity/table-id",
                "/api/entity/table-field",

                // BaseMapper CRUD
                "/api/mapper/insert",
                "/api/mapper/select-by-id",
                "/api/mapper/update-by-id",
                "/api/mapper/delete-by-id",

                // IService CRUD
                "/api/service/save",
                "/api/service/save-or-update",
                "/api/service/list",
                "/api/service/page",

                // 条件构造器
                "/api/wrapper/eq-like",
                "/api/wrapper/between-order",
                "/api/wrapper/lambda",
                "/api/wrapper/nested",

                // 分页
                "/api/page/basic",
                "/api/page/custom",

                // 高级注解
                "/api/advanced/logic-delete",
                "/api/advanced/optimistic-lock",
                "/api/advanced/auto-fill",

                // 批量操作
                "/api/batch/save-batch",
                "/api/batch/update-batch",

                // 综合实战
                "/api/realworld/user-order",
                "/api/realworld/status-stats",
                "/api/realworld/search-page",

                // 更多注解
                "/api/more/key-sequence",
                "/api/more/order-by",
                "/api/more/enum-value",
                "/api/more/interceptor-ignore",
                "/api/more/field-select",
                "/api/more/field-condition",
                "/api/more/field-update",
                "/api/more/field-numeric-scale",
                "/api/more/id-types",
                "/api/more/field-strategy",
                "/api/more/order",
                "/api/more/custom-annotation",
                "/api/more/accessors",

                // 扩展实战
                "/api/extension/type-handler",
                "/api/extension/active-record",
                "/api/extension/dynamic-table-name",
                "/api/extension/insert-batch-some-column",
                "/api/extension/chain-wrapper",
                "/api/extension/wrapper-advanced",
                "/api/extension/select-maps",
                "/api/extension/wrapper-update-delete"
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

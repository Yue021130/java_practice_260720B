package com.example.cg;

import com.example.cg.common.ApiResponse;
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
                "/api/list/arraylist-vs-linkedlist?size=10000",
                "/api/list/sublist-trap",
                "/api/list/listiterator",
                "/api/list/iterator-failfast",

                "/api/set/hashset-dedup",
                "/api/set/linkedhashset-order",
                "/api/set/treeset-sort",
                "/api/set/equals-hashcode-contract",

                "/api/maphash/hashmap-internals",
                "/api/maphash/key-mutation",
                "/api/maphash/map-compare",

                "/api/mapordered/linkedhashmap-lru?capacity=5",
                "/api/mapordered/treemap-sort",
                "/api/mapordered/weakhashmap-cache",

                "/api/queue/priorityqueue-topk?k=5",
                "/api/queue/arraydeque-dual",

                "/api/utils/sort-binarysearch",
                "/api/utils/synchronized-unmodifiable",
                "/api/utils/arrays-aslist-trap",
                "/api/utils/shuffle",

                "/api/generics/pecs",
                "/api/generics/type-erasure",
                "/api/generics/generic-method",
                "/api/generics/generic-dao",

                "/api/realworld/lru-cache?capacity=3",
                "/api/realworld/topk-words?k=5",
                "/api/realworld/groupby",
                "/api/realworld/comparator-sort"
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

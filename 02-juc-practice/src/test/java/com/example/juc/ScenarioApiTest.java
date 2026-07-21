package com.example.juc;

import com.example.juc.common.ApiResponse;
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
 * 遍历所有 32 个 POST 端点，验证返回 HTTP 200、统一响应 code=200、data 非空。
 * 这些接口本身会驱动多线程场景运行，既能覆盖接口连通性，也能暴露并发代码的基本问题。
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
                "/api/locks/transfer-deadlock?mode=trylock",
                "/api/locks/fair-vs-nonfair?threads=4",
                "/api/locks/interruptible",
                "/api/locks/readwrite-cache?readers=4&writers=1",
                "/api/locks/stamped-optimistic",
                "/api/locks/condition-buffer?producers=2&consumers=2&items=6",
                "/api/locks/locksupport",

                "/api/atomic/stock-compare?threads=4&times=1000",
                "/api/atomic/longadder-vs-atomic?threads=8&times=10000",
                "/api/atomic/aba",
                "/api/atomic/order-state",

                "/api/container/hashmap-accident?threads=4&keys=1000",
                "/api/container/chm-ops",
                "/api/container/cow-whitelist",
                "/api/container/delayqueue-order?orders=3&timeoutMs=800",
                "/api/container/blockingqueue-family",
                "/api/container/skiplist-leaderboard?players=10",

                "/api/sync/latch-startup",
                "/api/sync/barrier-report?rounds=1",
                "/api/sync/semaphore-limit?requests=4&permits=2",
                "/api/sync/exchanger-reconcile",

                "/api/future/cf-detail",
                "/api/future/old-future",
                "/api/future/forkjoin-sum?size=1000000",
                "/api/future/scheduled-compare",

                "/api/threadlocal/trace-chain",
                "/api/threadlocal/leak-demo",
                "/api/threadlocal/pool-context",

                "/api/base/volatile-visibility",
                "/api/base/volatile-not-atomic?threads=4&times=100",
                "/api/base/dcl-singleton",
                "/api/base/deadlock-detect"
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

package com.example.threadpool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ThreadPoolController 接口测试。
 *
 * 使用 MockMvc：不启动真实 HTTP 服务器，直接在内存中模拟请求，
 * 速度快且能覆盖「路由 → 参数绑定 → 业务 → JSON 序列化」全链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ThreadPoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/pool/metrics 返回 200，且包含三个池及其指标字段")
    void metrics应返回三个线程池的指标() throws Exception {
        mockMvc.perform(get("/api/pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 三个池都必须出现在返回列表中
                .andExpect(jsonPath("$.data[*].poolName",
                        hasItems("cpuPool", "ioPool", "customPool")))
                // 抽查关键指标字段存在（以列表第一个元素为代表）
                .andExpect(jsonPath("$.data[0].corePoolSize").exists())
                .andExpect(jsonPath("$.data[0].maxPoolSize").exists())
                .andExpect(jsonPath("$.data[0].activeCount").exists())
                .andExpect(jsonPath("$.data[0].poolSize").exists())
                .andExpect(jsonPath("$.data[0].queueSize").exists())
                .andExpect(jsonPath("$.data[0].queueCapacity").exists())
                .andExpect(jsonPath("$.data[0].completedTaskCount").exists())
                .andExpect(jsonPath("$.data[0].rejectedCount").exists());
    }

    @Test
    @DisplayName("POST submit 合法参数返回成功")
    void submit合法参数应成功() throws Exception {
        // 向 ioPool 提交 2 个 10ms 的小任务，避免给共享上下文带来压力
        mockMvc.perform(post("/api/pool/ioPool/submit")
                        .param("count", "2")
                        .param("taskDurationMs", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.requested").value(2))
                .andExpect(jsonPath("$.data.submitted").value(2));
    }

    @Test
    @DisplayName("POST submit 非法 poolName 返回 400")
    void submit非法池名应返回400() throws Exception {
        mockMvc.perform(post("/api/pool/notExistPool/submit")
                        .param("count", "1")
                        .param("taskDurationMs", "10"))
                .andExpect(status().isOk()) // 统一响应体：HTTP 200，业务码 400
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST resize 非法参数（core > max）返回 400")
    void resize核心数大于最大数应返回400() throws Exception {
        mockMvc.perform(post("/api/pool/ioPool/resize")
                        .param("corePoolSize", "10")
                        .param("maxPoolSize", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("maxPoolSize 必须 >= corePoolSize"));
    }
}

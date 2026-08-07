package com.example.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全场景接口集成测试（模拟发送模式）。
 *
 * 默认 simulate 模式：不连接 SMTP，只构造消息并记录，测试快速且无外部依赖。
 * retry-base-delay 调小，避免重试退避拖慢测试。
 */
@SpringBootTest(properties = {
        "mail.practice.mode=simulate",
        "mail.practice.retry-base-delay-ms=10"
})
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void basicScenarios() throws Exception {
        okPost("/api/basic/text");
        okPost("/api/basic/multiple");
        ok("/api/basic/recent");
    }

    @Test
    void htmlScenarios() throws Exception {
        okPost("/api/html/send");
        ok("/api/html/example");
    }

    @Test
    void attachmentScenarios() throws Exception {
        okPost("/api/attachment/csv");
        okPost("/api/attachment/image");
        ok("/api/attachment/limitations");
    }

    @Test
    void inlineScenarios() throws Exception {
        okPost("/api/inline/send");
        ok("/api/inline/compare");
    }

    @Test
    void templateScenarios() throws Exception {
        okPost("/api/template/welcome");
        okPost("/api/template/order");
        ok("/api/template/variables");
    }

    @Test
    void asyncScenarios() throws Exception {
        // 提交异步任务
        MvcResult submit = mockMvc.perform(post("/api/async/send"))
                .andExpect(status().isOk())
                .andReturn();
        String body = submit.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":200");

        // 解析 taskId，轮询直到 SENT（模拟模式应瞬间完成）
        String taskId = extractTaskId(body);
        assertThat(taskId).isNotBlank();
        boolean sent = false;
        for (int i = 0; i < 50; i++) {
            MvcResult statusResult = mockMvc.perform(get("/api/async/status").param("taskId", taskId))
                    .andExpect(status().isOk())
                    .andReturn();
            if (statusResult.getResponse().getContentAsString().contains("\"SENT\"")) {
                sent = true;
                break;
            }
            Thread.sleep(20);
        }
        assertThat(sent).as("异步任务最终应进入 SENT 状态").isTrue();

        ok("/api/async/pool");
    }

    @Test
    void retryScenarios() throws Exception {
        // failTimes=0：一次成功；failTimes=1 + 退避调小：重试后成功
        okPost("/api/retry/send?failTimes=0&maxRetries=3&backoff=fixed");
        okPost("/api/retry/send?failTimes=1&maxRetries=2&backoff=exponential");
        ok("/api/retry/strategy");
    }

    @Test
    void scheduleScenarios() throws Exception {
        okPost("/api/schedule/batch?count=3");
        okPost("/api/schedule/register?delaySeconds=0");
        ok("/api/schedule/list");
        ok("/api/schedule/heartbeat");
    }

    @Test
    void quartzScenarios() throws Exception {
        // 用 2099 年远期的 cron，避免测试期间真的触发；用 .param 传入避免 URL 编码问题
        String jobName = "test-job";
        mockMvc.perform(post("/api/schedule/quartz/register")
                        .param("jobName", jobName)
                        .param("cron", "0 0 12 31 12 ? 2099")
                        .param("subject", "测试任务"))
                .andExpect(status().isOk());
        ok("/api/schedule/quartz/list");
        okPost("/api/schedule/quartz/pause?jobName=" + jobName);
        okPost("/api/schedule/quartz/resume?jobName=" + jobName);
        okPost("/api/schedule/quartz/delete?jobName=" + jobName);
        ok("/api/schedule/quartz/explain");
    }

    @Test
    void eventScenarios() throws Exception {
        okPost("/api/event/send");
        okPost("/api/event/publish-demo");
        ok("/api/event/stats");
        ok("/api/event/listeners");
        ok("/api/event/explain");
    }

    @Test
    void headerScenarios() throws Exception {
        okPost("/api/header/send");
        okPost("/api/header/encoding");
        ok("/api/header/rules");
    }

    @Test
    void pitfallScenarios() throws Exception {
        ok("/api/pitfall/list");
        ok("/api/pitfall/plain-vs-html");
        ok("/api/pitfall/tuning");
    }

    private String extractTaskId(String body) {
        Matcher m = Pattern.compile("\"taskId\"\\s*:\\s*\"([0-9a-f]+)\"").matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private void ok(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":200");
    }

    private void okPost(String url) throws Exception {
        MvcResult result = mockMvc.perform(post(url))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":200");
    }
}

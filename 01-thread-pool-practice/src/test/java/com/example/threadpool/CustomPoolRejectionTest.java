package com.example.threadpool;

import com.example.threadpool.service.TaskSubmitService;
import com.example.threadpool.support.CountingRejectedHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 核心教学点验证：customPool（core=2, max=4, 队列=5）被打满后，
 * 超出容量的任务会触发 CountingRejectedHandler，计数上涨，
 * 且能通过 metrics 接口观察到。
 *
 * 稳定性说明：
 * - 任务的提交与「入队/扩容/拒绝」的判定都发生在调用线程内（同步），
 *   因此 submitTasks 返回时拒绝计数已经确定，不依赖异步时序；
 * - 任务耗时取 1000ms，远大于提交 30 个任务所需的微秒级时间，
 *   保证提交期间没有任务提前完成腾出位置；
 * - 断言用「提交后计数 > 提交前计数」而非固定值，兼容共享上下文中
 *   其他测试可能已经触发过拒绝的情况。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomPoolRejectionTest {

    @Autowired
    private TaskSubmitService taskSubmitService;

    @Autowired
    private CountingRejectedHandler customRejectedHandler;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("向 customPool 灌入远超容量的任务后，拒绝计数上涨且 metrics 可见")
    void 打满customPool应触发拒绝计数() throws Exception {
        int before = customRejectedHandler.getRejectedCount();

        // customPool 最多同时容纳 4(运行) + 5(排队) = 9 个任务，
        // 提交 30 个必然触发拒绝
        taskSubmitService.submitTasks("customPool", 30, 1000);

        // 拒绝判定是同步的，计数应立即上涨；加一次短等待兜底，避免极端机器抖动
        int after = waitRejectedCountAtLeast(before + 1, 3000);
        assertThat(after).as("提交 30 个任务后拒绝计数应上涨").isGreaterThan(before);

        // metrics 接口也应反映出拒绝计数（customPool 的 rejectedCount 字段）
        // 用 JsonPath 过滤出 customPool 的 rejectedCount，结果是一个单元素列表
        mockMvc.perform(get("/api/pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.poolName=='customPool')].rejectedCount",
                        hasItem(greaterThan(0))));
    }

    /** 轮询等待拒绝计数达到目标值，超时返回当前值（由调用方断言兜底） */
    private int waitRejectedCountAtLeast(int target, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int count;
        while ((count = customRejectedHandler.getRejectedCount()) < target
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        return customRejectedHandler.getRejectedCount();
    }
}

package com.example.tlp.task;

import com.example.tlp.trace.MdcTaskDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批次任务进度单测：真实线程池跑短任务，验证进度统计。
 */
class BatchTaskServiceTest {

    private ThreadPoolTaskExecutor executor;

    private BatchTaskService newService() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-batch-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return new BatchTaskService(executor);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void waitFinished(TaskProgress progress) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!progress.isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(progress.isFinished(), "10 秒内应执行完毕");
    }

    @Test
    @DisplayName("全部成功：done == total，failed == 0")
    void allSuccess() throws Exception {
        BatchTaskService service = newService();
        String taskId = service.submit(10, 20, 0);
        TaskProgress progress = service.getProgress(taskId);
        waitFinished(progress);

        assertEquals(10, progress.getTotal());
        assertEquals(10, progress.getDone().get());
        assertEquals(0, progress.getFailed().get());
        assertEquals(100, progress.getPercent());
    }

    @Test
    @DisplayName("全部失败（1000‰）：failed == total，进度仍算走完")
    void allFailed() throws Exception {
        BatchTaskService service = newService();
        String taskId = service.submit(5, 10, 1000);
        TaskProgress progress = service.getProgress(taskId);
        waitFinished(progress);

        assertEquals(0, progress.getDone().get());
        assertEquals(5, progress.getFailed().get());
        assertTrue(progress.isFinished());
    }

    @Test
    @DisplayName("未知 taskId 返回 null（前端轮询需判空）")
    void unknownTask() {
        BatchTaskService service = newService();
        assertNull(service.getProgress("not-exist"));
    }
}

package com.example.threadpool;

import com.example.threadpool.support.CountingRejectedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CountingRejectedHandler 纯单元测试（不起 Spring 容器，速度最快）。
 *
 * 直接构造一个真实的小线程池，手动调用 handler.rejectedExecution(...)，
 * 验证计数器按预期递增。
 */
class CountingRejectedHandlerTest {

    private CountingRejectedHandler handler;
    private ThreadPoolExecutor tinyPool;

    @BeforeEach
    void setUp() {
        handler = new CountingRejectedHandler();
        // 构造一个 1/1/队列1 的真实线程池，仅为给 handler 提供 executor 参数
        tinyPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1));
    }

    @AfterEach
    void tearDown() {
        tinyPool.shutdownNow();
    }

    @Test
    @DisplayName("初始计数为 0")
    void 初始计数应为0() {
        assertThat(handler.getRejectedCount()).isZero();
    }

    @Test
    @DisplayName("每触发一次 rejectedExecution，计数加 1")
    void 触发拒绝后计数应递增() {
        handler.rejectedExecution(() -> { }, tinyPool);
        assertThat(handler.getRejectedCount()).isEqualTo(1);

        handler.rejectedExecution(() -> { }, tinyPool);
        handler.rejectedExecution(() -> { }, tinyPool);
        assertThat(handler.getRejectedCount()).isEqualTo(3);
    }
}

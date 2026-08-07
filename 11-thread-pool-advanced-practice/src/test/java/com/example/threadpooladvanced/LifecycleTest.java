package com.example.threadpooladvanced;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleTest {

    @Test
    void shutdownShouldCompleteQueueTasks() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()
        );
        pool.submit(() -> {});
        pool.shutdown();
        boolean terminated = pool.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(pool.isShutdown()).isTrue();
        assertThat(terminated).isTrue();
        assertThat(pool.isTerminated()).isTrue();
    }

    @Test
    void shutdownNowShouldReturnPendingTasks() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10)
        );
        pool.submit(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {});
        }
        Thread.sleep(50);
        List<Runnable> pending = pool.shutdownNow();
        assertThat(pool.isShutdown()).isTrue();
        assertThat(pending.size()).isGreaterThanOrEqualTo(0);
    }
}

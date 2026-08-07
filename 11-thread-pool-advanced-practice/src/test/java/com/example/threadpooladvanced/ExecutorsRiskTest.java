package com.example.threadpooladvanced;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutorsRiskTest {

    @Test
    void fixedThreadPoolUsesUnboundedLinkedBlockingQueue() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        assertThat(pool.getQueue()).isInstanceOf(LinkedBlockingQueue.class);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(2);
        pool.shutdown();
    }

    @Test
    void cachedThreadPoolHasIntegerMaxMaxPoolSize() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        assertThat(pool.getCorePoolSize()).isZero();
        assertThat(pool.getMaximumPoolSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(pool.getQueue()).isInstanceOf(SynchronousQueue.class);
        pool.shutdown();
    }

    @Test
    void singleThreadExecutorDemonstratesUnboundedQueue() throws Exception {
        ExecutorService service = Executors.newSingleThreadExecutor();
        // 单线程池 + 无界队列，提交大量任务不会触发拒绝策略
        for (int i = 0; i < 5000; i++) {
            service.submit(() -> {});
        }
        service.shutdown();
        boolean terminated = service.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();
    }

    @Test
    void scheduledThreadPoolHasIntegerMaxMaxPoolSize() {
        ScheduledThreadPoolExecutor pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(pool.getQueue().getClass().getSimpleName()).isEqualTo("DelayedWorkQueue");
        pool.shutdown();
    }
}

package com.example.tlp.task;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批次任务进度快照：内存态，供前端轮询。
 * 单机内存方案足够演示；集群部署应换 Redis 共享。
 *
 * <p>done/failed 用 AtomicInteger：子任务在多个线程池线程里并发累加，
 * 普通 int 的 read-modify-write 会丢更新（读到相同值各自 +1），
 * 导致进度永远到不了 100%——这是并发计数器的经典坑。</p>
 */
@Data
public class TaskProgress {

    private String taskId;
    private String traceId;
    private int total;
    private final AtomicInteger done = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private LocalDateTime startTime;

    public int getPercent() {
        return total == 0 ? 0 : (done.get() + failed.get()) * 100 / total;
    }

    public boolean isFinished() {
        return done.get() + failed.get() >= total;
    }
}

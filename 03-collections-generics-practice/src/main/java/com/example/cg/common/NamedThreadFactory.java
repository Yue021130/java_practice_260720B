package com.example.cg.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 命名线程工厂：给场景线程起有意义的名字。
 *
 * 线程名是并发可视化的核心——前端时间线按线程名着色，
 * 日志和 jstack 里也能一眼认出是哪个场景的线程。
 * （面试点：生产项目务必自定义线程名前缀，不要用 pool-1-thread-1 默认名。）
 *
 * 所有线程设为 daemon：教学场景中线程都是短命的，
 * 即使某个场景演示了「停不下来」的坑，也不会挡住 JVM 退出。
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger seq = new AtomicInteger(1);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, prefix + seq.getAndIncrement());
        t.setDaemon(true);
        return t;
    }
}

package com.example.async.task;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 演示 Spring AOP 代理坑：同类内部调用 this.inner() 会绕过代理，导致 @Async 不生效。
 */
@Service
public class SelfInvocationService {

    /**
     * 外部入口：内部直接调用 inner()，不会走 Spring 代理，因此 inner() 仍在当前线程同步执行。
     */
    public String outer() {
        String outerThread = Thread.currentThread().getName();
        // 这里直接 this.inner()，等价于非代理方法调用
        String innerThread = inner().join();
        return outerThread + "|" + innerThread;
    }

    @Async
    public CompletableFuture<String> inner() {
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }
}

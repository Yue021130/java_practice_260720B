package com.example.tlp.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MDC 透传装饰器单测：异步线程拿到提交时刻的 traceId，执行后清理。
 */
class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    @DisplayName("透传：装饰后的任务在另一个「线程」里仍能看到原 traceId")
    void transmit() {
        TraceContext.set("trace-abc");
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> seen.set(TraceContext.get()));
        // 模拟线程切换：先清掉当前线程的 MDC 再执行装饰任务
        TraceContext.clear();
        decorated.run();
        assertEquals("trace-abc", seen.get());
    }

    @Test
    @DisplayName("清理：任务执行完 MDC 被清空，线程可安全复用")
    void clearAfterRun() {
        TraceContext.set("trace-xyz");
        Runnable decorated = decorator.decorate(() -> assertEquals("trace-xyz", TraceContext.get()));
        TraceContext.clear();
        decorated.run();
        assertNull(TraceContext.get(), "finally clear 后线程复用不会串号");
    }

    @Test
    @DisplayName("提交时 MDC 为空也能执行（不 NPE）")
    void nullContextSafe() {
        TraceContext.clear();
        Runnable decorated = decorator.decorate(() -> assertNull(TraceContext.get()));
        assertDoesNotThrow(decorated::run);
    }
}

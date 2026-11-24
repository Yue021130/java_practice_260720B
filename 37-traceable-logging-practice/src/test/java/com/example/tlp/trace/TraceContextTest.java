package com.example.tlp.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceContext 单测：新链路开辟、嵌套还原、清理。
 */
class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    @DisplayName("runWithNewTrace：开辟新 traceId，执行完清理")
    void newTraceAndClear() {
        assertNull(TraceContext.get());
        TraceContext.runWithNewTrace(() -> {
            assertNotNull(TraceContext.get());
            assertEquals(32, TraceContext.get().length(), "UUID 去横线后 32 位");
        });
        assertNull(TraceContext.get(), "执行完必须清理，否则线程复用串号");
    }

    @Test
    @DisplayName("runWithNewTrace：嵌套调用恢复原值")
    void nestedRestore() {
        TraceContext.set("outer-trace");
        TraceContext.runWithNewTrace(() -> {
            assertNotEquals("outer-trace", TraceContext.get());
        });
        assertEquals("outer-trace", TraceContext.get(), "嵌套结束后应恢复外层 traceId");
    }
}

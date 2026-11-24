package com.example.tlp.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceIdFilter 单测：透传沿用、缺失生成、响应回写、finally 清理。
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    @DisplayName("上游传 X-Trace-Id：沿用并回写响应头")
    void passThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.HEADER, "upstream-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals("upstream-123", response.getHeader(TraceContext.HEADER));
        assertNull(TraceContext.get(), "过滤器必须清理 MDC");
    }

    @Test
    @DisplayName("未传 traceId：生成新的并回写")
    void generateWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        String traceId = response.getHeader(TraceContext.HEADER);
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
        assertNull(TraceContext.get());
    }
}

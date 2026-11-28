package com.example.prod.unit;

import com.example.prod.security.RateLimitFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试（金字塔 70% 层）：限流过滤器。
 * 容量 3、每分钟补充 3：前 3 个请求放行，第 4 个 429。
 */
class RateLimitFilterTest {

    @Test
    @DisplayName("令牌耗尽前放行，耗尽后 429")
    void limit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3, 3);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        AtomicInteger passed = new AtomicInteger();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
                passed.incrementAndGet();
            }
        };

        int ok = 0;
        int tooMany = 0;
        String retryAfter = null;
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            if (response.getStatus() == 429) {
                tooMany++;
                retryAfter = response.getHeader("Retry-After");
            } else {
                ok++;
            }
        }
        assertEquals(3, ok, "容量 3，前 3 个应放行");
        assertEquals(2, tooMany, "后 2 个应被限流");
        assertEquals(3, passed.get());
        assertEquals("60", retryAfter, "429 应带 Retry-After 提示");
    }

    @Test
    @DisplayName("不同 IP 独立计数")
    void perIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        MockHttpServletRequest a = new MockHttpServletRequest();
        a.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse ra = new MockHttpServletResponse();
        filter.doFilter(a, ra, new MockFilterChain());

        MockHttpServletRequest b = new MockHttpServletRequest();
        b.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse rb = new MockHttpServletResponse();
        filter.doFilter(b, rb, new MockFilterChain());

        assertNotEquals(429, ra.getStatus());
        assertNotEquals(429, rb.getStatus(), "不同 IP 的令牌桶互不影响");
    }
}

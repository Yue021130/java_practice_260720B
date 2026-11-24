package com.example.tlp.trace;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求入口的 traceId 过滤器：
 *
 * <ol>
 *   <li>上游（前端/网关）传了 {@code X-Trace-Id} 就沿用——全链路串联
 *   的前提是每一段都透传同一个 id；</li>
 *   <li>没传则生成新的，并回写到响应头——前端拿返回值就能去日志里
 *   精确检索本次请求的所有日志；</li>
 *   <li>finally 清理 MDC——防 Tomcat 线程复用串号。</li>
 * </ol>
 *
 * <p>用 OncePerRequestFilter 而不是实现 Filter：forward/include
 * 导致的二次进入不会重复生成 traceId。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(TraceContext.HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = TraceContext.generate();
        }
        TraceContext.set(traceId);
        response.setHeader(TraceContext.HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 清理不是可选动作：线程池复用下不清理 = 下一个请求串用旧 traceId
            MDC.remove(TraceContext.TRACE_ID);
        }
    }
}

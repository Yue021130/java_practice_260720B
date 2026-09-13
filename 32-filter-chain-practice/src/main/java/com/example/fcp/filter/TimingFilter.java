package com.example.fcp.filter;

import com.example.fcp.entity.RequestLog;
import com.example.fcp.service.RequestLogService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 请求耗时审计过滤器：统计每个请求的处理耗时与状态码，写入 sys_request_log。
 *
 * <p>注册方式三：FilterRegistrationBean（order=10），由 {@code FilterConfig} 注册。
 * 注意本类<strong>不能</strong>标 @Component——否则 Spring Boot 会自动再注册一次导致重复执行；
 * 同理通过构造器注入 Service，而不是 @Autowired 字段。</p>
 *
 * <p>核心设计（与第31章 Interceptor 审计的关键差异）：TimingFilter 位于
 * SaServletFilter（order=20，登录校验）<strong>之前</strong>，用 try/finally 包绕
 * {@code chain.doFilter()}——即使后续鉴权失败（401）或抛异常，finally 依然会落库。
 * 第31章 LogInterceptor 的 afterCompletion 在 SaInterceptor preHandle 抛异常时不会触发，
 * 因此记不到鉴权失败的请求；Filter 层审计天然覆盖全量请求。</p>
 *
 * <p>继承 {@link OncePerRequestFilter}：一次请求只执行一次，
 * 避免 ERROR dispatch（如 404/500 转发到 /error）或 async 场景导致重复计时落库。</p>
 */
@Slf4j
public class TimingFilter extends OncePerRequestFilter {

    private final RequestLogService requestLogService;

    public TimingFilter(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 登录接口与文档路径不记录，减少噪音
        return "/api/auth/login".equals(uri)
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/doc.html")
                || uri.startsWith("/webjars")
                || uri.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        Throwable throwable = null;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException e) {
            throwable = e;
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            record(request, response, cost, throwable);
        }
    }

    private void record(HttpServletRequest request, HttpServletResponse response, long cost, Throwable throwable) {
        try {
            RequestLog requestLog = new RequestLog();
            requestLog.setTraceId(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
            requestLog.setIp(getClientIp(request));
            requestLog.setMethod(request.getMethod());
            requestLog.setUri(request.getRequestURI());
            requestLog.setStatusCode(response.getStatus());
            requestLog.setCostMs((int) cost);
            requestLog.setHasError(throwable != null || response.getStatus() >= 500 ? 1 : 0);
            requestLog.setCreateTime(LocalDateTime.now());
            requestLogService.save(requestLog);
            log.info("[TimingFilter] {} {} cost={}ms status={}",
                    requestLog.getMethod(), requestLog.getUri(), cost, requestLog.getStatusCode());
        } catch (Exception e) {
            // 审计落库失败不影响主流程
            log.warn("[TimingFilter] 审计日志写入失败: {}", e.getMessage());
        }
    }

    /** 获取客户端真实 IP（考虑反向代理常见请求头） */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_CLIENT_IP"};
        for (String h : headers) {
            String ip = request.getHeader(h);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

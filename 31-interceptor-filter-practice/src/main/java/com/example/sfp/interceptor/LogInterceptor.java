package com.example.sfp.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.example.sfp.entity.ApiLog;
import com.example.sfp.service.ApiLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

/**
 * 操作审计拦截器：记录每一次 Controller 请求的「IP + 用户 + URI + 耗时 + 状态码」。
 *
 * <p>八股：Interceptor 属于 Spring MVC，只对被 DispatcherServlet 路由到的 Handler 生效；
 * afterCompletion 在视图渲染/异常处理完成后调用，ex 参数可以感知异常。</p>
 */
@Component
@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_KEY = "LOG_START_TIME";

    @Resource
    private ApiLogService apiLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long start = (Long) request.getAttribute(START_TIME_KEY);
        long cost = start != null ? System.currentTimeMillis() - start : 0L;

        ApiLog apiLog = new ApiLog();
        apiLog.setIp(getClientIp(request));
        apiLog.setMethod(request.getMethod());
        apiLog.setUri(request.getRequestURI());
        apiLog.setUserAgent(request.getHeader("User-Agent"));
        apiLog.setUsername(getLoginUsername());
        apiLog.setStatusCode(response.getStatus());
        apiLog.setCostMs((int) cost);
        apiLog.setHasError(ex != null || response.getStatus() >= 500 ? 1 : 0);
        apiLog.setCreateTime(LocalDateTime.now());

        apiLogService.save(apiLog);
        log.info("[Interceptor] 请求审计 {} {} cost={}ms status={}",
                apiLog.getMethod(), apiLog.getUri(), cost, apiLog.getStatusCode());
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

    /** 当前登录用户名；未登录则记为 anonymous */
    private String getLoginUsername() {
        try {
            if (StpUtil.isLogin()) {
                Object loginId = StpUtil.getLoginId();
                return loginId != null ? loginId.toString() : "anonymous";
            }
        } catch (Exception ignored) {
            // Sa-Token 未登录时会抛异常，这里静默处理
        }
        return "anonymous";
    }
}

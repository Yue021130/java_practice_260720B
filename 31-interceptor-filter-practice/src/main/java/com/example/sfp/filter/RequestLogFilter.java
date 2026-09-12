package com.example.sfp.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求日志 Filter：在 Servlet 层最前端执行，打印请求进入服务器的时间与基础信息，
 * 并把 Request 包装为可重复读取的 {@link RepeatedlyReadHttpServletRequestWrapper}。
 *
 * <p>八股：Filter 属于 Servlet 规范，对所有请求生效（包括静态资源、错误页、/api 接口）；
 * 这里演示「包装 Request」和「Filter 层最早日志」两种典型用法。
 * 业务日志记录（耗时、状态码、操作用户）放在 Spring MVC 的 Interceptor 中更合适。</p>
 */
@Slf4j
public class RequestLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 包装为可重复读，避免后续读取 body 时报 stream closed
        RepeatedlyReadHttpServletRequestWrapper wrapper =
                new RepeatedlyReadHttpServletRequestWrapper(httpRequest);

        String uri = wrapper.getRequestURI();
        String method = wrapper.getMethod();
        String contentType = wrapper.getContentType();
        log.info("[Filter] 请求进入 {} {}，Content-Type={}", method, uri, contentType);

        // 仅对 JSON 请求打印 body（避免打印二进制或文件流）
        if (isJsonContent(contentType)) {
            String body = wrapper.getBodyString();
            if (!body.isEmpty()) {
                log.info("[Filter] 请求体：{}", body.length() > 1000 ? body.substring(0, 1000) + "..." : body);
            }
        }

        chain.doFilter(wrapper, response);
    }

    private boolean isJsonContent(String contentType) {
        return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
    }
}

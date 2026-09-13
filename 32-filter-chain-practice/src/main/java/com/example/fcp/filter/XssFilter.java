package com.example.fcp.filter;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * XSS 防护过滤器：包装 Request，对参数与请求体做 HTML 转义。
 *
 * <p>注册方式二：@WebFilter + 启动类 @ServletComponentScan——
 * 这是 Servlet 规范的标准注解方式，无需任何 Spring 配置类。</p>
 *
 * <p>八股：@WebFilter 没有 order 属性，执行顺序由容器按 FilterName/声明顺序决定，
 * 无法精确控制。因此本 Filter 设计上与顺序无关（清洗不依赖 traceId/鉴权结果），
 * 生产环境若需精确排序请改用 FilterRegistrationBean。</p>
 */
@Slf4j
@WebFilter(urlPatterns = "/api/*")
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(httpRequest);
        log.debug("[XssFilter] 请求已包装清洗 {} {}", wrapper.getMethod(), wrapper.getRequestURI());
        chain.doFilter(wrapper, response);
    }
}

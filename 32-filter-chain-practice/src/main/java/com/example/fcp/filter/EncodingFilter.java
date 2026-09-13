package com.example.fcp.filter;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 字符编码过滤器：统一请求/响应编码为 UTF-8。
 *
 * <p>注册方式三：由 FilterConfig 通过 FilterRegistrationBean 注册（order=2），
 * 并演示 init-param 的读取——Filter 生命周期 init(FilterConfig) 在应用启动时
 * 被容器调用一次，适合读取初始化参数。</p>
 *
 * <p>八股：Spring Boot 默认已注册 CharacterEncodingFilter（强制请求编码），
 * 本类用于演示 Filter 生命周期与 init-param 机制，生产直接用内置的即可。</p>
 */
@Slf4j
public class EncodingFilter implements Filter {

    private String encoding = StandardCharsets.UTF_8.name();

    @Override
    public void init(FilterConfig filterConfig) {
        String param = filterConfig.getInitParameter("encoding");
        if (param != null && !param.isEmpty()) {
            this.encoding = param;
        }
        log.info("[EncodingFilter] init，encoding={}", encoding);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(encoding);
        }
        response.setCharacterEncoding(encoding);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("[EncodingFilter] destroy");
    }
}

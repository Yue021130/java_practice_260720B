package com.example.sfp.config;

import com.example.sfp.filter.RequestLogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 Servlet Filter：请求日志 Filter 需要最早进入链路，因此 order 设为 1。
 *
 * <p>八股：Filter 由 Servlet 容器管理，通过 FilterRegistrationBean 可以精确控制
 * 顺序（order 越小越先执行）、URL 匹配规则以及是否初始化参数。</p>
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RequestLogFilter> requestLogFilter() {
        FilterRegistrationBean<RequestLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLogFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("requestLogFilter");
        return registration;
    }
}

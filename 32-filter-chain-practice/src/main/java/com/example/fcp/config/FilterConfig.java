package com.example.fcp.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.example.fcp.common.Result;
import com.example.fcp.filter.EncodingFilter;
import com.example.fcp.filter.TimingFilter;
import com.example.fcp.service.RequestLogService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter 注册中心（注册方式三：FilterRegistrationBean，可精确控制顺序）。
 *
 * <p>本章过滤器链全景（执行顺序由小到大）：
 * <ol>
 *   <li>TraceIdFilter —— @Component + @Order(1)，见 filter 包；</li>
 *   <li>EncodingFilter —— 本类注册，order=2，演示 init-param；</li>
 *   <li>XssFilter —— @WebFilter + @ServletComponentScan，见 filter 包；</li>
 *   <li>TimingFilter —— 本类注册，order=10，OncePerRequestFilter 耗时审计；</li>
 *   <li>SaServletFilter —— 本类注册，order=20，Filter 层登录校验。</li>
 * </ol>
 * TimingFilter 在 SaServletFilter 之前，用 finally 保证鉴权失败的 401 也落审计表。</p>
 *
 * <p>八股：被 @Component 标注的 Filter 会被 Spring Boot 自动注册一次；
 * 若再用 FilterRegistrationBean 注册同一个 Bean 会执行两次——
 * 因此 TimingFilter/EncodingFilter 均不标 @Component，由本类 {@code new} 出来注册。</p>
 */
@Configuration
public class FilterConfig {

    /** 字符编码过滤器：order=2，演示 init-param 读取（Filter 生命周期 init 阶段） */
    @Bean
    public FilterRegistrationBean<EncodingFilter> encodingFilter() {
        FilterRegistrationBean<EncodingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new EncodingFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2);
        registration.setName("encodingFilter");
        registration.addInitParameter("encoding", "UTF-8");
        return registration;
    }

    /** 耗时审计过滤器：order=10，位于鉴权之前，finally 兜底记录全部请求（含 401） */
    @Bean
    public FilterRegistrationBean<TimingFilter> timingFilter(RequestLogService requestLogService) {
        FilterRegistrationBean<TimingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TimingFilter(requestLogService));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(10);
        registration.setName("timingFilter");
        return registration;
    }

    /**
     * Sa-Token 登录校验过滤器：order=20，Filter 层鉴权（对应经典 AuthenticationFilter 场景）。
     *
     * <p>注意：Filter 在 DispatcherServlet 之前执行，@RestControllerAdvice 捕不到这里的异常，
     * 因此用 setError 回调直接写统一的 401 Result。</p>
     */
    @Bean
    public FilterRegistrationBean<SaServletFilter> saServletFilter() {
        FilterRegistrationBean<SaServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SaServletFilter()
                .addInclude("/api/**")
                .addExclude("/api/auth/login")
                .setAuth(obj -> SaRouter.match("/api/**")
                        .notMatch("/api/auth/login")
                        .check(r -> StpUtil.checkLogin()))
                .setError(e -> Result.fail(401, "未登录或登录已过期，请重新登录")));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(20);
        registration.setName("saServletFilter");
        return registration;
    }
}

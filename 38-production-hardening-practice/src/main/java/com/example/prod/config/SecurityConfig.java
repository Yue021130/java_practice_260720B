package com.example.prod.config;

import com.example.prod.security.JwtAuthFilter;
import com.example.prod.security.SecurityUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 主配置（Security 5.7 新式 SecurityFilterChain Bean 写法，
 * WebSecurityConfigurerAdapter 在 5.7 已废弃）。
 *
 * <p>生产加固点全部落在这里：</p>
 * <ol>
 *   <li>无状态会话：SessionCreationPolicy.STATELESS，服务端不存会话；</li>
 *   <li>白名单最小化：只放行登录与健康检查，其余一律认证（默认拒绝）；</li>
 *   <li>CORS 指定具体域名——allowedOrigins 用 * 等于放弃跨域防护；</li>
 *   <li>安全响应头：frame/frameOptions 禁用（防点击劫持）等；</li>
 *   <li>@EnableGlobalMethodSecurity：开启 @PreAuthorize 方法级权限
 *   （Boot 2.7 用此注解；Boot 3 改名 @EnableMethodSecurity）；</li>
 *   <li>JWT 过滤器加在 UsernamePasswordAuthenticationFilter 之前；
 *   限流过滤器在更外层（RateLimitFilter @Order 更高）。</li>
 * </ol>
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 无状态：不使用 HttpSession
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // CSRF 对无状态 JWT 无意义，关闭；若加 Cookie 会话必须重新评估
            .csrf().disable()
            // 【加固】CORS 白名单域名，禁止 *
            .cors().configurationSource(corsSource())
            .and()
            .headers()
                .frameOptions().deny()          // 防点击劫持
                .contentTypeOptions().and()     // X-Content-Type-Options: nosniff
            .and()
            .authorizeRequests(auth -> auth
                // 白名单：登录 + 健康检查（最小化，宁漏不放）
                .antMatchers("/api/auth/login", "/actuator/health").permitAll()
                // 其余一律认证（默认拒绝）
                .anyRequest().authenticated())
            // 未认证 → 401 JSON（不配会跳转表单登录页）
            .exceptionHandling()
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":401,\"message\":\"未认证或 Token 无效\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":403,\"message\":\"无权限访问\"}");
                })
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS：指定具体来源、方法、头，禁止通配 */
    private CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 本地开发前端地址；生产改为真实域名清单
        config.setAllowedOrigins(Collections.singletonList("http://localhost:5173"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Trace-Id"));
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 登录认证：DaoAuthenticationProvider（UserDetailsService + BCrypt） */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(SecurityUserDetailsService uds,
                                                            PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }
}

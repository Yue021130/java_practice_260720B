package com.example.prod.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器：Spring Security 链路的入口。
 *
 * <ol>
 *   <li>解析 Authorization: Bearer &lt;token&gt;；</li>
 *   <li>校验签名/过期 + 黑名单（被登出撤销的直接 401）；</li>
 *   <li>加载用户与权限，写入 SecurityContextHolder——后续
 *   @PreAuthorize 的 hasAuthority 判定就靠它；</li>
 *   <li>finally clearContext()：SecurityContext 默认存在线程上，
 *   Tomcat 线程复用不清理会串号/越权——与 MDC 清理同理。</li>
 * </ol>
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService,
                         TokenBlacklist tokenBlacklist,
                         UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = jwtService.extractToken(header);
                Jws<Claims> jws = jwtService.parse(token);
                Claims claims = jws.getBody();

                // 黑名单检查：登出即失效
                if (tokenBlacklist.contains(claims.getId())) {
                    log.warn("Token 已被撤销 jti={}", claims.getId());
                    SecurityContextHolder.clearContext();
                } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
                    List<SimpleGrantedAuthority> authorities = user.getAuthorities().stream()
                            .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                            .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 认证通过 user={} authorities={}", claims.getSubject(), authorities);
                }
            } catch (Exception e) {
                // 任何解析失败都不放行：清上下文，让 EntryPoint 统一 401
                log.debug("JWT 校验失败：{}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // 防线程池复用污染：与 MDC finally clear 同理
            SecurityContextHolder.clearContext();
        }
    }
}

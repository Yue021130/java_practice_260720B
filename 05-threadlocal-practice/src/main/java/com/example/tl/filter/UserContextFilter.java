package com.example.tl.filter;

import com.example.tl.context.UserContext;
import com.example.tl.context.dto.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求级上下文 Filter：进入时从 Header 解析用户，结束时 remove。
 *
 * 面试八股：
 * - Web 场景下 ThreadLocal 生命周期必须和请求绑定
 * - 在 Filter/Interceptor 的 finally 中 remove 是标准做法
 * - 忘记 remove 会导致线程池复用时拿到上一个请求的用户（串号）
 */
@WebFilter(urlPatterns = "/api/*")
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String userId = req.getHeader("X-User-Id");
        String userName = req.getHeader("X-User-Name");

        if (userId != null && !userId.isEmpty()) {
            UserContext.set(new User(Long.valueOf(userId), userName));
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}

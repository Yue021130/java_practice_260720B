package com.example.caa.aspect;

import com.example.caa.annotation.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限校验切面。
 *
 * <p>@Before 前置通知：在目标方法执行前校验权限。
 * 本示例从请求头 X-Role 中读取角色，并与注解要求的权限做匹配。</p>
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    /**
     * 模拟角色权限映射：admin 拥有所有权限，user 只有 user:view。
     */
    private static final Set<String> ADMIN_PERMS = new HashSet<>(Arrays.asList("admin", "user:view", "user:edit"));
    private static final Set<String> USER_PERMS = new HashSet<>(Collections.singletonList("user:view"));

    @Before("@annotation(requirePermission)")
    public void before(RequirePermission requirePermission) {
        String required = requirePermission.value();
        String currentRole = getCurrentRole();

        log.info("[权限校验] 当前角色: {}, 需要权限: {}", currentRole, required);

        boolean hasPermission;
        if ("admin".equalsIgnoreCase(currentRole)) {
            hasPermission = ADMIN_PERMS.contains(required);
        } else if ("user".equalsIgnoreCase(currentRole)) {
            hasPermission = USER_PERMS.contains(required);
        } else {
            hasPermission = false;
        }

        if (!hasPermission) {
            throw new SecurityException("没有权限: " + required);
        }
    }

    private String getCurrentRole() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "anonymous";
        }
        HttpServletRequest request = attributes.getRequest();
        String role = request.getHeader("X-Role");
        return role == null || role.trim().isEmpty() ? "anonymous" : role.trim();
    }
}

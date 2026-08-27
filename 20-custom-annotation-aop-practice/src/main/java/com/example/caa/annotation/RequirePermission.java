package com.example.caa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解。
 *
 * <p>标注在方法上，方法执行前会校验当前用户是否拥有指定权限。
 * 无权限时抛出 SecurityException，由全局异常处理器返回 403。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限标识，如 "admin"、"user:edit"。
     */
    String value();
}

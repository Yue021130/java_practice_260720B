package com.example.caa.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * 可重复注解：审计记录。
 *
 * <p>对应微信公众号原文“加餐一”：Java 8 开始允许同一个注解多次使用，
 * 需要配合 @Repeatable 指定容器注解。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Audits.class)
public @interface Audit {

    /**
     * 审计动作，如 "CREATE_USER"、"UPDATE_ORDER"。
     */
    String action();

    /**
     * 审计说明。
     */
    String desc() default "";
}

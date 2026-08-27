package com.example.caa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解。
 *
 * <p>标注在方法上，AOP 切面会自动记录方法名、参数、返回值、耗时、是否异常等信息。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 操作类型，如 "查询用户"、"创建订单"。
     */
    String value() default "";

    /**
     * 是否记录方法参数。
     */
    boolean logParams() default true;

    /**
     * 是否记录返回值。
     */
    boolean logResult() default true;
}

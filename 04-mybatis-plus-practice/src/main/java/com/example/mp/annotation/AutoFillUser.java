package com.example.mp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解：自动填充当前操作人。
 *
 * 配合 AOP 切面使用，标注在 Service 方法上，
 * 可在 insert/update 前自动给实体的 createBy / updateBy 赋值。
 *
 * 面试点：
 * - 自定义注解用 @interface 定义
 * - @Target 限定可用位置（方法、类等）
 * - @Retention 控制保留级别（RUNTIME 才能被反射/AOP 读取）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFillUser {

    /**
     * 模拟当前用户，实际项目可从 SecurityContext / ThreadLocal 取。
     */
    String value() default "system";
}

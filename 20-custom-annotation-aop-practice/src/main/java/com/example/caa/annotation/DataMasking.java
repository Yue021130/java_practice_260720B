package com.example.caa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解。
 *
 * <p>标注在方法上，方法返回后 AOP 切面对返回值对象中的指定字段做脱敏处理。
 * 支持字符串类型字段，如手机号、邮箱、身份证等。</p>
 *
 * <p>文章提醒：反射处理字段性能较低，大数据量时慎用；切面修改返回值对调用方不透明，
 * 必要时提供开关（如本注解的 fields 为空则跳过）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataMasking {

    /**
     * 需要脱敏的字段名数组。
     */
    String[] fields() default {};

    /**
     * 脱敏类型：PHONE（手机号）、EMAIL（邮箱）、DEFAULT（默认中间替换为 ***）。
     */
    MaskType maskType() default MaskType.DEFAULT;

    enum MaskType {
        PHONE,
        EMAIL,
        DEFAULT
    }
}

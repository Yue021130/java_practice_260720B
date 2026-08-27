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

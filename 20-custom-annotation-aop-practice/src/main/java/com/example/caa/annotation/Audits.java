package com.example.caa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Audit} 的容器注解。
 *
 * <p>容器注解必须包含一个类型为 Audit[] 的 value() 方法，且其 @Retention 和 @Target
 * 通常与可重复注解保持一致。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audits {

    Audit[] value();
}

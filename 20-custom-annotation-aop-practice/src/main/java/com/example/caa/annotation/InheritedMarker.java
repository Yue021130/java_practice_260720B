package com.example.caa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 演示元注解 @Inherited。
 *
 * <p>加上 @Inherited 后，子类可以继承父类上的该注解（仅对类继承有效，对接口实现无效）。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface InheritedMarker {

    /**
     * 标记值，用于反射验证。
     */
    String value() default "inherited";
}

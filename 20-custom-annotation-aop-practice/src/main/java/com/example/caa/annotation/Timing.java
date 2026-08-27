package com.example.caa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法耗时监控注解。
 *
 * <p>标注在方法上，AOP 切面会计算方法执行耗时并输出日志，
 * 同时把耗时信息附加到返回结果中。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timing {

    /**
     * 方法描述，用于日志标识。
     */
    String value() default "";
}

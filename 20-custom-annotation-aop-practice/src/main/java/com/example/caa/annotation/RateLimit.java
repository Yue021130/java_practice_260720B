package com.example.caa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解。
 *
 * <p>基于内存计数器实现简易限流，适合教学演示。
 * 生产环境建议使用 Redis + Lua 或 Sentinel。</p>
 *
 * <p>文章坑点提醒：注解参数语义要清晰，如这里用 {@link TimeUnit} 枚举表示时间单位，
 * 避免用魔法数字 0 造成“不过期还是立即过期”的歧义。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 单位时间内允许的最大请求数。
     */
    int qps() default 10;

    /**
     * 时间窗口长度。
     */
    long window() default 1;

    /**
     * 时间窗口单位。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流触发后的提示信息。
     */
    String message() default "请求过于频繁，请稍后再试";
}

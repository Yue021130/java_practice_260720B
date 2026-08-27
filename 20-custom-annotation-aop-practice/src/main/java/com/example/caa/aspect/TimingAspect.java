package com.example.caa.aspect;

import com.example.caa.annotation.Timing;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 方法耗时监控切面。
 *
 * <p>@Around 环绕通知：计算方法执行耗时，输出日志，并把耗时信息附加到 Map 类型的返回值中。</p>
 */
@Slf4j
@Aspect
@Component
public class TimingAspect {

    @Around("@annotation(timing)")
    public Object around(ProceedingJoinPoint joinPoint, Timing timing) throws Throwable {
        long start = System.currentTimeMillis();
        String desc = timing.value().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : timing.value();

        try {
            return joinPoint.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("[耗时监控] {} 执行耗时: {} ms", desc, cost);
        }
    }
}

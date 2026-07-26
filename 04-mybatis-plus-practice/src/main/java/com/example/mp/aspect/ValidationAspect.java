package com.example.mp.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AOP 校验切面：演示 @Order 优先级。
 *
 * 面试点：
 * - @Order 数字越小，优先级越高，越先执行。
 * - 多个切面同时拦截同一方法时，@Order 控制它们的执行顺序。
 */
@Slf4j
@Aspect
@Component
@Order(10)
public class ValidationAspect {

    @Before("@annotation(com.example.mp.annotation.AutoFillUser)")
    public void before(JoinPoint joinPoint) {
        log.info("[ValidationAspect @Order(10)] 先执行参数校验：{}" , joinPoint.getSignature().getName());
    }
}

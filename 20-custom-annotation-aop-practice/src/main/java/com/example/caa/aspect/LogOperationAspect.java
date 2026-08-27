package com.example.caa.aspect;

import com.example.caa.annotation.LogOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面。
 *
 * <p>@Around 环绕通知：在目标方法执行前后记录日志，包括方法名、参数、返回值、耗时、异常信息。
 * 这种方式可以获得最完整的上下文。</p>
 */
@Slf4j
@Aspect
@Component
public class LogOperationAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
        long start = System.currentTimeMillis();
        String operation = logOperation.value().isEmpty()
                ? joinPoint.getSignature().getName()
                : logOperation.value();

        Map<String, Object> logMap = new HashMap<>();
        logMap.put("operation", operation);
        logMap.put("method", joinPoint.getSignature().toShortString());

        if (logOperation.logParams()) {
            logMap.put("params", joinPoint.getArgs());
        }

        Object result;
        boolean success = true;
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
            if (logOperation.logResult()) {
                logMap.put("result", result);
            }
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            logMap.put("error", errorMsg);
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            logMap.put("costMs", cost);
            logMap.put("success", success);
            log.info("[操作日志] {}", toJson(logMap));
        }

        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}

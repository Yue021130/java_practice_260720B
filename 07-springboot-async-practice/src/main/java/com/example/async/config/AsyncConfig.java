package com.example.async.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 启用 Spring 异步代理并指定默认线程池与全局异常处理器。
 * <p>proxyTargetClass = true 表示强制使用 CGLIB 代理，避免同类内部调用 this 绕过代理时
 * 因接口代理导致的类型转换问题（但 this 调用本身仍不走代理）。
 */
@Slf4j
@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig implements AsyncConfigurer {

    @Resource
    private ThreadPoolTaskExecutor defaultPool;

    @Override
    public Executor getAsyncExecutor() {
        return defaultPool;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("异步方法执行异常：{}.{}，参数：{}，异常：{}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        params,
                        ex.getMessage(),
                        ex);
            }
        };
    }
}

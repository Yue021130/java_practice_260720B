package com.example.async.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 配置 Spring MVC 异步支持使用的 TaskExecutor，避免使用默认 SimpleAsyncTaskExecutor。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource(name = "defaultPool")
    private ThreadPoolTaskExecutor defaultPool;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(defaultPool);
        configurer.setDefaultTimeout(30000);
    }
}

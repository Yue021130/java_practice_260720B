package com.example.sbcore.conditional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OnMissingBeanConfig {

    @Bean
    @ConditionalOnMissingBean(name = "demoCacheManager")
    public String fallbackCacheManager() {
        return "fallback-cache-manager";
    }
}

package com.example.sbcore.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StarterService {

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, String> starters = new LinkedHashMap<>();
        starters.put("spring-boot-starter-web", "自动引入 Tomcat、Spring MVC、Jackson、HTTP 消息转换器");
        starters.put("spring-boot-starter-validation", "引入 Hibernate Validator，支持 JSR-303/380 校验");
        starters.put("spring-boot-starter-cache", "引入 Spring 缓存抽象，需配合具体实现（Caffeine/Redis）");
        starters.put("spring-boot-starter-data-redis", "引入 Spring Data Redis、Lettuce 连接池、RedisTemplate");
        starters.put("spring-boot-starter-actuator", "暴露 /actuator/* 运行期端点，支持健康检查与指标");

        data.put("starters", starters);
        data.put("starterCount", starters.size());
        data.put("interviewNote",
                "Starter 是‘依赖 + 自动配置 + 默认约定’的组合包。引入 starter 后，Maven/Gradle 拉取依赖，" +
                "Spring Boot 通过 @EnableAutoConfiguration 读取 META-INF/spring.factories 中的配置类，" +
                "配合 @ConditionalOnClass/@ConditionalOnMissingBean 等条件装配完成自动配置。");

        return data;
    }
}

package com.example.sbcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConfigPriorityService {

    @Autowired
    private Environment environment;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("serverPort", environment.getProperty("server.port"));
        data.put("appName", environment.getProperty("app.name"));
        data.put("activeProfiles", environment.getActiveProfiles());
        data.put("defaultProfiles", environment.getDefaultProfiles());

        Map<String, String> priority = new LinkedHashMap<>();
        priority.put("1", "命令行参数（最高）");
        priority.put("2", "SPRING_APPLICATION_JSON");
        priority.put("3", "ServletConfig / ServletContext 初始化参数");
        priority.put("4", "JNDI 属性");
        priority.put("5", "系统环境变量（如 APP_NAME）");
        priority.put("6", "RandomValuePropertySource");
        priority.put("7", "application-{profile}.yml（指定 profile）");
        priority.put("8", "application.yml 默认配置");
        priority.put("9", "@PropertySource");
        priority.put("10", "默认值（最低）");

        data.put("priorityOrder", priority);
        data.put("interviewNote",
                "Spring Boot 配置优先级从高到低：命令行参数 > SPRING_APPLICATION_JSON > Servlet/环境变量 > " +
                "application-{profile}.yml > application.yml > @PropertySource > 默认值。" +
                "profile 通过 spring.profiles.active 激活，同名配置后者覆盖前者。");

        return data;
    }
}

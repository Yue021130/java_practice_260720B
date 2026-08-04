package com.example.sbcore.service;

import com.example.sbcore.property.AppProperties;
import com.example.sbcore.property.CustomConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConfigPropsService {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CustomConfigProperties customConfigProperties;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, Object> app = new LinkedHashMap<>();
        app.put("name", appProperties.getName());
        app.put("version", appProperties.getVersion());
        app.put("userName", appProperties.getUser().getUserName());
        app.put("age", appProperties.getUser().getAge());
        app.put("email", appProperties.getUser().getEmail());

        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("appId", customConfigProperties.getAppId());
        custom.put("threadPoolSize", customConfigProperties.getThreadPoolSize());
        custom.put("enabled", customConfigProperties.getEnabled());

        data.put("appProperties", app);
        data.put("customProperties", custom);

        Map<String, String> relaxedBinding = new LinkedHashMap<>();
        relaxedBinding.put("配置项 user-name", "绑定到 userName");
        relaxedBinding.put("配置项 user_name", "绑定到 userName");
        relaxedBinding.put("配置项 USER_NAME", "绑定到 userName");
        data.put("relaxedBindingExamples", relaxedBinding);

        data.put("interviewNote",
                "@ConfigurationProperties 把前缀相同的配置批量绑定到 Java Bean，支持宽松绑定（kebab/snake/upper case）。" +
                "配合 @Validated 和 JSR-303 注解（@NotNull、@Range）可在启动时完成校验，避免非法配置进入容器。");

        return data;
    }
}

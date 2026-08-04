package com.example.sbcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConditionalService {

    @Autowired
    private ApplicationContext applicationContext;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        boolean onFeature = applicationContext.containsBean("onFeatureBean");
        boolean onClass = applicationContext.containsBean("onClassBean");
        boolean onMissing = applicationContext.containsBean("fallbackCacheManager");

        data.put("onFeatureBeanRegistered", onFeature);
        data.put("onFeatureReason", "feature.demo=true 或缺省（@ConditionalOnProperty）");

        data.put("onClassBeanRegistered", onClass);
        data.put("onClassReason", "类路径存在 org.springframework.data.redis.core.RedisTemplate（@ConditionalOnClass）");

        data.put("onMissingBeanRegistered", onMissing);
        data.put("onMissingBeanReason", "容器中不存在名为 demoCacheManager 的 Bean（@ConditionalOnMissingBean）");

        data.put("interviewNote",
                "条件装配让自动配置类按需生效：@ConditionalOnProperty 按配置项判断；" +
                "@ConditionalOnClass 按类路径判断；@ConditionalOnMissingBean 防止重复注册。" +
                "只有条件全部满足，配置类或 Bean 才会真正注册到 Spring 容器。");

        return data;
    }
}

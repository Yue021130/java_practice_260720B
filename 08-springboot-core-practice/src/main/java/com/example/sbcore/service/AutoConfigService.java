package com.example.sbcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoConfigService {

    @Autowired
    private ApplicationContext applicationContext;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        List<Map<String, String>> beans = new ArrayList<>();
        addBean(beans, "dispatcherServlet", "org.springframework.web.servlet.DispatcherServlet");
        addBean(beans, "requestMappingHandlerAdapter", "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter");
        addBean(beans, "requestMappingHandlerMapping", "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping");
        addBean(beans, "errorAttributes", "org.springframework.boot.web.servlet.error.ErrorAttributes");
        addBean(beans, "caffeineCacheManager", "org.springframework.cache.CacheManager");
        addBean(beans, "redisCacheManager", "org.springframework.cache.CacheManager");
        addBean(beans, "healthEndpoint", "org.springframework.boot.actuate.health.HealthEndpoint");

        data.put("autoConfigBeans", beans);
        data.put("springFactoriesCount", countAutoConfigurations());
        data.put("interviewNote",
                "自动装配入口：@SpringBootApplication -> @EnableAutoConfiguration。" +
                "Spring Boot 启动时读取所有 jar 包中 META-INF/spring.factories 的 " +
                "org.springframework.boot.autoconfigure.EnableAutoConfiguration 键值，得到候选配置类；" +
                "再通过 @ConditionalOnClass、@ConditionalOnMissingBean、@ConditionalOnProperty 过滤，最终生效。");

        return data;
    }

    private void addBean(List<Map<String, String>> list, String name, String type) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("exists", String.valueOf(applicationContext.containsBean(name)));
        list.add(map);
    }

    private int countAutoConfigurations() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:META-INF/spring.factories");
            int count = 0;
            CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
            for (Resource resource : resources) {
                java.util.Properties props = new java.util.Properties();
                props.load(resource.getInputStream());
                String value = props.getProperty("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
                if (value != null) {
                    for (String cls : value.split(",")) {
                        cls = cls.trim();
                        if (cls.isEmpty()) continue;
                        try {
                            MetadataReader reader = factory.getMetadataReader(cls);
                            if (reader.getAnnotationMetadata().hasAnnotation("org.springframework.context.annotation.Configuration")) {
                                count++;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }
}

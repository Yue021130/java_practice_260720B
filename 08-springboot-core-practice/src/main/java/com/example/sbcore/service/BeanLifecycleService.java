package com.example.sbcore.service;

import com.example.sbcore.lifecycle.DemoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BeanLifecycleService {

    @Autowired
    private DemoBean demoBean;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        String order = demoBean.resetAndShow();
        List<String> steps = Arrays.asList(order.split(";"));

        data.put("lifecycleSteps", steps);
        data.put("interviewNote",
                "Spring Bean 生命周期扩展点顺序：" +
                "1) 构造器实例化 -> 2) 属性赋值 -> 3) Aware 接口回调 -> 4) BeanPostProcessor.before -> " +
                "5) @PostConstruct -> 6) InitializingBean.afterPropertiesSet -> 7) init-method -> " +
                "8) BeanPostProcessor.after -> Bean 就绪；容器关闭时：@PreDestroy -> DisposableBean.destroy -> destroy-method。");

        return data;
    }
}

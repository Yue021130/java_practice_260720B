package com.example.sbcore.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DemoBean) {
            DemoBean.ORDER.append("5.BeanPostProcessor.postProcessBeforeInitialization;");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DemoBean) {
            DemoBean.ORDER.append("9.BeanPostProcessor.postProcessAfterInitialization;");
        }
        return bean;
    }
}

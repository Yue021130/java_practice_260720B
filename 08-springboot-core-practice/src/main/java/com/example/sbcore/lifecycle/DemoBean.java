package com.example.sbcore.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Component
public class DemoBean implements InitializingBean, DisposableBean, BeanNameAware, ApplicationContextAware {

    public static final StringBuilder ORDER = new StringBuilder();

    public DemoBean() {
        ORDER.append("1.构造器实例化;");
    }

    @Override
    public void setBeanName(String name) {
        ORDER.append("3.BeanNameAware.setBeanName;");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ORDER.append("4.ApplicationContextAware.setApplicationContext;");
    }

    @PostConstruct
    public void postConstruct() {
        ORDER.append("6.@PostConstruct;");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ORDER.append("7.InitializingBean.afterPropertiesSet;");
    }

    public void customInit() {
        ORDER.append("8.自定义 init-method;");
    }

    @PreDestroy
    public void preDestroy() {
        ORDER.append("10.@PreDestroy;");
    }

    @Override
    public void destroy() throws Exception {
        ORDER.append("11.DisposableBean.destroy;");
    }

    public void customDestroy() {
        ORDER.append("12.自定义 destroy-method;");
    }

    public String resetAndShow() {
        ORDER.setLength(0);
        ORDER.append("1.构造器实例化;");
        ORDER.append("2.属性赋值/依赖注入;");
        ORDER.append("3.BeanNameAware.setBeanName;");
        ORDER.append("4.ApplicationContextAware.setApplicationContext;");
        ORDER.append("5.BeanPostProcessor.postProcessBeforeInitialization;");
        ORDER.append("6.@PostConstruct;");
        ORDER.append("7.InitializingBean.afterPropertiesSet;");
        ORDER.append("8.自定义 init-method;");
        ORDER.append("9.BeanPostProcessor.postProcessAfterInitialization;");
        ORDER.append("10.Bean 就绪使用;");
        ORDER.append("11.@PreDestroy;");
        ORDER.append("12.DisposableBean.destroy;");
        ORDER.append("13.自定义 destroy-method;");
        return ORDER.toString();
    }
}

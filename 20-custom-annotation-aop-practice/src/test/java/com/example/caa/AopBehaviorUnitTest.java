package com.example.caa;

import com.example.caa.annotation.DataMasking;
import com.example.caa.annotation.LogOperation;
import com.example.caa.annotation.RateLimit;
import com.example.caa.annotation.RequirePermission;
import com.example.caa.annotation.Timing;
import com.example.caa.demo.DemoService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AOP 行为单元测试。
 */
@SpringBootTest
public class AopBehaviorUnitTest {

    @Autowired
    private DemoService demoService;

    @Test
    void demoServiceShouldBeAopProxy() {
        // DemoService 被 AOP 代理后，AopUtils.isAopProxy 返回 true
        assertThat(AopUtils.isAopProxy(demoService)).isTrue();
    }

    @Test
    void annotationsShouldBePresent() throws NoSuchMethodException {
        Method adminMethod = DemoService.class.getMethod("adminOnly");
        assertThat(adminMethod.isAnnotationPresent(RequirePermission.class)).isTrue();
        assertThat(adminMethod.getAnnotation(RequirePermission.class).value()).isEqualTo("admin");

        Method rateLimitMethod = DemoService.class.getMethod("rateLimit");
        RateLimit rateLimit = rateLimitMethod.getAnnotation(RateLimit.class);
        assertThat(rateLimit).isNotNull();
        assertThat(rateLimit.qps()).isEqualTo(2);
        assertThat(rateLimit.timeUnit()).isEqualTo(TimeUnit.SECONDS);

        Method logMethod = DemoService.class.getMethod("getUser", Long.class);
        LogOperation logOperation = logMethod.getAnnotation(LogOperation.class);
        assertThat(logOperation).isNotNull();
        assertThat(logOperation.value()).isEqualTo("查询用户详情");

        Method timingMethod = DemoService.class.getMethod("timing");
        assertThat(timingMethod.isAnnotationPresent(Timing.class)).isTrue();

        Method maskingMethod = DemoService.class.getMethod("maskingUser");
        DataMasking dataMasking = maskingMethod.getAnnotation(DataMasking.class);
        assertThat(dataMasking).isNotNull();
        assertThat(dataMasking.fields()).contains("phone", "email", "idCard");
    }

    @Test
    void targetClassShouldBeDemoService() {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(demoService);
        assertThat(targetClass).isEqualTo(DemoService.class);
    }
}

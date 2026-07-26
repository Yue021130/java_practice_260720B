package com.example.mp.aspect;

import com.example.mp.annotation.AutoFillUser;
import com.example.mp.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义注解 @AutoFillUser 的 AOP 实现：自动填充 createBy / updateBy。
 *
 * 面试点：
 * - @Aspect + @Component 声明切面
 * - @Before("@annotation(xxx)") 拦截带注解的方法
 * - 通过反射修改方法参数中的实体字段
 * - 实际项目中当前用户通常从 SecurityContext / ThreadLocal 获取
 */
@Slf4j
@Aspect
@Component
@Order(20)
public class AutoFillUserAspect {

    @Before("@annotation(com.example.mp.annotation.AutoFillUser)")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFillUser annotation = method.getAnnotation(AutoFillUser.class);
        String operator = annotation.value();

        log.info("[AutoFillUserAspect @Order(20)] 后执行自动填充，当前操作人：{}" , operator);

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Task) {
                Task task = (Task) arg;
                if (task.getId() == null) {
                    // 新增
                    task.setCreateBy(operator);
                    task.setUpdateBy(operator);
                    task.setCreateTime(LocalDateTime.now());
                    task.setUpdateTime(LocalDateTime.now());
                } else {
                    // 更新
                    task.setUpdateBy(operator);
                    task.setUpdateTime(LocalDateTime.now());
                }
            }
        }
    }
}

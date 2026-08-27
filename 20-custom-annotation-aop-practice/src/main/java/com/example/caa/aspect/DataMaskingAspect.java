package com.example.caa.aspect;

import com.example.caa.annotation.DataMasking;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

/**
 * 数据脱敏切面。
 *
 * <p>@AfterReturning 返回后通知：在方法正常返回后，对返回值中的敏感字段做脱敏。
 * 支持单个对象和集合。</p>
 *
 * <p>文章提醒：切面里修改返回值对调用方不透明，调试困难；
 * 本切面通过反射处理字段，大数据量时建议改为序列化层统一处理或提供开关。</p>
 */
@Slf4j
@Aspect
@Component
public class DataMaskingAspect {

    @AfterReturning(pointcut = "@annotation(dataMasking)", returning = "result")
    public Object afterReturning(Object result, DataMasking dataMasking) {
        if (result == null) {
            return null;
        }

        String[] fields = dataMasking.fields();
        DataMasking.MaskType maskType = dataMasking.maskType();

        try {
            if (result instanceof Collection) {
                for (Object item : (Collection<?>) result) {
                    maskObject(item, fields, maskType);
                }
            } else {
                maskObject(result, fields, maskType);
            }
        } catch (Exception e) {
            log.warn("[数据脱敏] 脱敏处理失败", e);
        }

        return result;
    }

    private void maskObject(Object obj, String[] fields, DataMasking.MaskType maskType) throws IllegalAccessException {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        for (String fieldName : fields) {
            Field field;
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(obj);
            if (!(value instanceof String)) {
                continue;
            }
            String str = (String) value;
            String masked = mask(str, maskType);
            field.set(obj, masked);
        }
    }

    private String mask(String value, DataMasking.MaskType maskType) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        switch (maskType) {
            case PHONE:
                return maskPhone(value);
            case EMAIL:
                return maskEmail(value);
            case DEFAULT:
            default:
                return maskDefault(value);
        }
    }

    private String maskPhone(String phone) {
        if (phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "****" + email.substring(atIndex);
    }

    private String maskDefault(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        int start = value.length() / 4;
        int end = value.length() - start;
        return value.substring(0, start) + "****" + value.substring(end);
    }
}

package com.example.vmp.validation.annotation;

import com.example.vmp.validation.validator.IdCardValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 身份证号格式校验注解（18 位加权因子校验）。
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IdCardValidator.class)
public @interface IdCard {

    String message() default "身份证号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

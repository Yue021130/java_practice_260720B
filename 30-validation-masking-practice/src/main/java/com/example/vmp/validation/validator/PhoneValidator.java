package com.example.vmp.validation.validator;

import com.example.vmp.validation.annotation.Phone;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 手机号校验器：1 开头，第二位 3-9，共 11 位。
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final Pattern PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null/空串放行，交给 @NotBlank 处理必填
        if (value == null || value.isEmpty()) {
            return true;
        }
        return PATTERN.matcher(value).matches();
    }
}

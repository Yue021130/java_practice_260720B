package com.example.vmp.validation.validator;

import com.example.vmp.validation.annotation.IdCard;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 身份证号校验器：18 位，前 17 位数字，末位数字或 X，且通过加权因子校验。
 */
public class IdCardValidator implements ConstraintValidator<IdCard, String> {

    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (value.length() != 18) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            sum += (c - '0') * WEIGHTS[i];
        }
        char expected = CHECK_CODES[sum % 11];
        char actual = Character.toUpperCase(value.charAt(17));
        return expected == actual;
    }
}

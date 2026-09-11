package com.example.vmp.validation.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手机号校验器单元测试。
 */
class PhoneValidatorTest {

    private final PhoneValidator validator = new PhoneValidator();

    @Test
    void validPhones() {
        assertTrue(validator.isValid("13800138000", null));
        assertTrue(validator.isValid("13912345678", null));
        assertTrue(validator.isValid("18600001234", null));
    }

    @Test
    void nullOrEmpty() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
    }

    @Test
    void invalidPhones() {
        assertFalse(validator.isValid("12345678901", null));
        assertFalse(validator.isValid("1380013800", null));
        assertFalse(validator.isValid("138001380000", null));
        assertFalse(validator.isValid("12800138000", null));
    }
}

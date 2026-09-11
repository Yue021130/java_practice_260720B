package com.example.vmp.validation.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份证号校验器单元测试。
 */
class IdCardValidatorTest {

    private final IdCardValidator validator = new IdCardValidator();

    @Test
    void validIdCards() {
        assertTrue(validator.isValid("110101199003077758", null));
        assertTrue(validator.isValid("440305199207118833", null));
        assertTrue(validator.isValid("11010119871122331X", null));
    }

    @Test
    void nullOrEmpty() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
    }

    @Test
    void wrongLength() {
        assertFalse(validator.isValid("11010119900307775", null));
        assertFalse(validator.isValid("11010119900307775888", null));
    }

    @Test
    void wrongCheckCode() {
        assertFalse(validator.isValid("110101199003077757", null));
        assertFalse(validator.isValid("11010119900307775X", null));
    }

    @Test
    void nonDigitInFirst17() {
        assertFalse(validator.isValid("110101199003077A58", null));
    }
}
